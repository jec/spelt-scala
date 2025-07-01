package net.jcain.spelt.store

import com.google.inject.Guice
import neotypes.AsyncDriver
import net.jcain.spelt.models.Config
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RoomStoreSpec @Inject() (implicit driver: AsyncDriver[Future], xc: ExecutionContext) extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback(driver) {
  trait TargetActor {
    val store: ActorRef[RoomStore.Request] = testKit.spawn(Behaviors.setup(context => RoomStore(context, driver)))
    val probe: TestProbe[RoomStore.Response] = testKit.createTestProbe[RoomStore.Response]()
  }
  
  trait RoomRequestWithNones extends TargetActor {
    val roomRequest: CreateRoomRequest = CreateRoomRequest(
      name = Some("My New Room"),
      room_alias_name = Some("newalias"),
      topic = Some("This is a test.")
    )
  }

  "CreateRoom" when {
    "request with mostly Nones" should {
      "respond with CreateRoomResponse(Right(room))" in new RoomRequestWithNones {
        store ! RoomStore.CreateRoom(roomRequest, probe.ref)

        inside(probe.expectMessageType[RoomStore.Response]) {
          case RoomStore.CreateRoomResponse(Right(room)) =>
            ULID(room.identifier) shouldBe a [ULID]
            room.name shouldEqual roomRequest.name
            room.alias shouldEqual roomRequest.room_alias_name
            room.topic shouldEqual roomRequest.topic
            room.roomVersion shouldEqual Config.defaultNewRoomVersion
        }
      }
    }
  }
}
