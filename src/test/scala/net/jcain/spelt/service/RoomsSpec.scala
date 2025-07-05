package net.jcain.spelt.service

import net.jcain.spelt.models.{Config, Room, User}
import net.jcain.spelt.support.{MockEventStore, MockEvents, MockRoomStore}
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import wvlet.airframe.ulid.ULID


class RoomsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers:
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

        // Configure MockEventStore to answer with CreateEventsForNewRoomResponse(Right(_)).
        private val eventStore = testKit.spawn(MockEventStore())
        // Configure MockRoomStore to answer with CreateRoomResponse(Right(_)).
        private val roomStore = testKit.spawn(MockRoomStore(expectedRoom))
        private val rooms = testKit.spawn(Rooms(eventStore, roomStore))
        private val probe = testKit.createTestProbe[Rooms.Response]()
        private val user = User("phred", "foo", "phred@example.com")

        rooms ! Rooms.CreateRoom(request, user, probe.ref)

        inside(probe.expectMessageType[Rooms.Response]) {
          case Rooms.CreateRoomResponse(Right(room)) =>
        }
      }
    }
  }
