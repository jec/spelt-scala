package net.jcain.spelt.store

import net.jcain.spelt.models.Config
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

class RoomStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "CreateRoom" when {
    "request with mostly Nones" should {
      "respond with CreateRoomResponse(Right(room))" in {
        val repo = testKit.spawn(RoomStore())
        val probe = testKit.createTestProbe[RoomStore.Response]()

        val roomRequest = CreateRoomRequest(None,
          Seq(),
          Seq(),
          Seq(),
          None,
          Some("My New Room"),
          Seq(),
          None,
          Some("newalias"),
          None,
          Some("This is a test."),
          None,
          None)

        repo ! RoomStore.CreateRoom(roomRequest, probe.ref)

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
