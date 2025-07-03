package net.jcain.spelt.store

import net.jcain.spelt.models.User
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global

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

  "CreateEventsForNewRoom" when {
    "request is mostly Nones" should {
      "create the events and respond with success" in new RoomRequestWithNones {
        val user: User = User("phredsmerd", "foo", "phredsmerd@example.com")

        store ! EventStore.CreateEventsForNewRoom("foo", roomRequest, user, probe.ref)

        inside(probe.expectMessageType[EventStore.Response]) {
          case EventStore.CreateEventsForNewRoomResponse(Right(())) =>

        }
      }
    }
  }
