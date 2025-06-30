package net.jcain.spelt.service

import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.RoomStore
import net.jcain.spelt.support.{DatabaseRollback, MockRoomStore}
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}

class RoomsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait RoomRequestWithNones {
    val request: JsObject = Json.obj(
      "name" -> "My New Room",
      "room_alias_name" -> "newalias",
      "topic" -> "This is a test."
    )
  }

  "CreateRoom" when {
    "request with mostly Nones" should {
      "create Room and Event nodes" in new RoomRequestWithNones {
        private val roomStore = testKit.spawn(MockRoomStore())
        private val rooms = testKit.spawn(Rooms(roomStore))
        private val probe = testKit.createTestProbe[Rooms.Response]()

        rooms ! Rooms.CreateRoom(request, probe.ref)
      }
    }
  }
}
