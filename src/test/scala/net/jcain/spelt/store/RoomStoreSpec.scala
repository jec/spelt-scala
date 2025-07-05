package net.jcain.spelt.store

import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.c
import net.jcain.spelt.models.{Config, User}
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class RoomStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait TargetActor {
    val store: ActorRef[RoomStore.Request] = testKit.spawn(RoomStore())
    val probe: TestProbe[RoomStore.Response] = testKit.createTestProbe[RoomStore.Response]()
  }

  trait ExistingUser:
    val user: User = User("phredsmerd", "foo", "phredsmerd@example.com")

    private val result = Await.result(c"CREATE (u:User {$user})".execute.resultSummary(driver), 1.minute)

    result.counters.nodesCreated shouldEqual 1

  trait RoomRequestWithNones extends TargetActor with ExistingUser {
    val roomRequest: CreateRoomRequest = CreateRoomRequest(
      name = Some("My New Room"),
      room_alias_name = Some("newalias"),
      topic = Some("This is a test.")
    )
  }

  "CreateRoom" when {
    "request with mostly Nones" should {
      "respond with CreateRoomResponse(Right(room))" in new RoomRequestWithNones {
        store ! RoomStore.CreateRoom(roomRequest, user.name, probe.ref)

        inside(probe.expectMessageType[RoomStore.Response]) {
          case RoomStore.CreateRoomResponse(Right(room)) =>
            ULID(room.identifier) shouldBe a [ULID]
            room.name shouldEqual roomRequest.name
            room.alias shouldEqual roomRequest.room_alias_name
            room.topic shouldEqual roomRequest.topic
            room.roomVersion shouldEqual Config.defaultNewRoomVersion

            // Check Room node and relationship
            val result = Await.result(
              c"MATCH path = (u:User)-[:CREATED]->(r:Room) WHERE r.identifier = ${room.identifier} RETURN path"
                .query(ResultMapper.path)
                .single(driver),
              1.minute)

            println(result.segments.length)
            val head = result.segments.head
            println(head.start)
            println(head.relationship)
            println(head.end)

        }
      }
    }
  }
}
