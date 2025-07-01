package net.jcain.spelt.service

import net.jcain.spelt.models.{Config, Room}
import net.jcain.spelt.support.{MockEvents, MockRoomStore}
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import wvlet.airframe.ulid.ULID


class RoomsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
  trait MinimalRoomsRequest {
    val name = "My New Room"
    val alias = "newalias"
    val topic = "This is a test."

    val request: JsObject = Json.obj(
      "name" -> name,
      "room_alias_name" -> alias,
      "topic" -> topic
    )
  }

  "CreateRoom" when {
    "minimal attributes in request" should {
      "create Room and Event nodes" in new MinimalRoomsRequest {
        val expectedRoom: Room = Room(ULID.newULIDString, roomVersion = Config.defaultNewRoomVersion)

        // Configure MockEvents to answer with CreateEventsForNewRoomResponse(Right(_)).
        private val events = testKit.spawn(MockEvents())
        // Configure MockRoomStore to answer with CreateRoomResponse(Right(_)).
        private val roomStore = testKit.spawn(MockRoomStore(expectedRoom))
        private val rooms = testKit.spawn(Rooms(events, roomStore))
        private val probe = testKit.createTestProbe[Rooms.Response]()

        rooms ! Rooms.CreateRoom(request, probe.ref)

        inside(probe.expectMessageType[Rooms.Response]) {
          case Rooms.CreateRoomResponse(Right(room)) =>
        }
      }
    }
  }
}
