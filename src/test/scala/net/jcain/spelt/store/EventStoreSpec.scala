package net.jcain.spelt.store

import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.c
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.models.{Room, User}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class EventStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback:
  trait TargetActor:
    val store: ActorRef[EventStore.Request] = testKit.spawn(EventStore())
    val probe: TestProbe[EventStore.Response] = testKit.createTestProbe[EventStore.Response]()

  trait RoomRequestWithNones extends TargetActor {
    val roomRequest: CreateRoomRequest = CreateRoomRequest(
      name = Some("My New Room"),
      room_alias_name = Some("newalias"),
      topic = Some("This is a test.")
    )
  }

  trait ExistingUser:
    val user: User = User("phredsmerd", "foo", "phredsmerd@example.com")

    private val result = Await.result(c"CREATE (u:User {$user})".execute.resultSummary(driver), 1.minute)

    result.counters.nodesCreated shouldEqual 1

  trait ExistingRoom extends RoomRequestWithNones with ExistingUser:
    private val roomStore = testKit.spawn(RoomStore())
    private val roomStoreProbe = testKit.createTestProbe[RoomStore.Response]()

    roomStore ! RoomStore.CreateRoom(roomRequest, user.name, roomStoreProbe.ref)

    val response: RoomStore.CreateRoomResponse = roomStoreProbe.expectMessageType[RoomStore.CreateRoomResponse]

    val room: Room = response match {
      case RoomStore.CreateRoomResponse(Right(room)) =>
        room
    }

  "CreateEventsForNewRoom" when {
    "request is mostly Nones" should {
      "create the events and respond with success" in new ExistingRoom {
        store ! EventStore.CreateEventsForNewRoom(room.identifier, roomRequest, user, probe.ref)

        inside(probe.expectMessageType[EventStore.Response]) {
          case EventStore.CreateEventsForNewRoomResponse(Right(())) =>
            val result = Await.result(
              c"MATCH path = (r:Room)<-[:SENT_TO]-(e:Event) WHERE r.identifier = ${room.identifier} RETURN path"
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
