package net.jcain.spelt.service

import net.jcain.spelt.models.User
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.EventStore
import net.jcain.spelt.support.MockEventStore
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class EventsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers:
  "Foo" when {
    "minimal attributes in request" should {
      "respond with success" in {
        // Configure MockEventStore to answer with CreateEventsForNewRoomResponse(Right(_)).
        val eventStoreRef = testKit.spawn(MockEventStore())
        val subject = testKit.spawn(Events(eventStoreRef))
        val probe = testKit.createTestProbe[Events.Response]()

        val request = CreateRoomRequest(name = Some("My New Room"), room_alias_name = Some("newroom"), topic = Some("Testing a new room"))
        val user = User("phredsmerd", "foo", "phredsmerd@example.com")

//        subject ! Events.CreateEventsForNewRoom("foo", request, user, probe.ref)
      }
    }
  }
