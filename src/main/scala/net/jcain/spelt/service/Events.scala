package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.models.requests.CreateRoomRequest
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import play.api.libs.concurrent.ActorModule

object Events extends ActorModule:
  type Message = Request

  sealed trait Request
  final case class CreateEventsForNewRoom(roomId: String, request: CreateRoomRequest, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateEventsForNewRoomResponse(unitOrError: Either[String, Unit]) extends Response

  @Provides
  def apply(): Behavior[Request] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case CreateEventsForNewRoom(roomId, request, replyTo) =>
        createEventsForNewRoom(roomId, request, replyTo)
        Behaviors.same
    }
  }

  private def createEventsForNewRoom(roomId: String, request: CreateRoomRequest, replyTo: ActorRef[Response]): Unit =
    // Event m.room.create
    // Event m.room.member
    // Event m.room.power_levels
    // Event m.room.canonical_alias if `room_alias_name`
    // Events in `preset`
    // Events in `initial_state`
    // Event m.room_name if `name`
    // Event m.room.topic if `topic`
    // Events from `invite` and `invite_3pid`
    ()
