package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.json.Reads.requests.createRoomRequest
import net.jcain.spelt.models.Room
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.RoomStore
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout
import play.api.Logging
import play.api.libs.concurrent.ActorModule
import play.api.libs.json.*
import play.api.libs.json.Reads.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Rooms extends ActorModule with Logging:
  type Message = Request

  implicit val timeout: Timeout = 5.seconds

  // Actor messages
  sealed trait Request
  final case class CreateRoom(params: JsValue, replyTo: ActorRef[Response]) extends Request
  private final case class RoomCreated(room: Room, request: CreateRoomRequest, replyTo: ActorRef[Response]) extends Request
  private final case class RoomFailed(message: String, replyTo: ActorRef[Response]) extends Request
  private final case class RoomEventsCreated(room: Room, replyTo: ActorRef[Response]) extends Request
  private final case class RoomEventsFailed(message: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateRoomResponse(roomOrError: Either[String, Room]) extends Response

  // Receive `Request`s as well as `RoomStore.Response`s
  private type RequestAndResponse = Request | RoomStore.Response

  @Provides
  def apply(events: ActorRef[Events.Request],
            roomStore: ActorRef[RoomStore.Request]): Behavior[Request] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[Request] {
        case CreateRoom(params, replyTo) =>
          requestCreateRoom(params, roomStore, replyTo, context)
          Behaviors.same

        case RoomCreated(room, request, replyTo) =>
          requestCreateRoomEvents(room, request, events, replyTo, context)
          Behaviors.same

        case RoomFailed(message, replyTo) =>
          replyTo ! CreateRoomResponse(Left(message))
          Behaviors.same

        case RoomEventsCreated(room, replyTo) =>
          replyTo ! CreateRoomResponse(Right(room))
          Behaviors.same

        case RoomEventsFailed(message, replyTo) =>
          replyTo ! CreateRoomResponse(Left(message))
          Behaviors.same
      }
    }

  private def requestCreateRoomEvents(room: Room,
                                      request: CreateRoomRequest,
                                      events: ActorRef[Events.Request],
                                      replyTo: ActorRef[Response],
                                      context: ActorContext[Request]): Unit =
    context.ask(events, ref => Events.CreateEventsForNewRoom(room.identifier, request, ref)) {
      case Success(Events.CreateEventsForNewRoomResponse(Right(()))) =>
        RoomEventsCreated(room, replyTo)
      case Failure(error) =>
        RoomEventsFailed(error.getMessage, replyTo)
    }

  /**
   * Validates `params` and requests `RoomStore` to create a Room node
   *
   * @param params HTTP request body from POST create_room
   * @param roomStore
   * @param replyTo
   * @param context
   */
  private def requestCreateRoom(params: JsValue,
                                roomStore: ActorRef[RoomStore.Request],
                                replyTo: ActorRef[Response],
                                context: ActorContext[Request]): Unit =
    params.validate[CreateRoomRequest] match {
      case JsSuccess(request, _) =>
        context.ask(roomStore, ref => RoomStore.CreateRoom(request, ref)) {
          case Success(RoomStore.CreateRoomResponse(Right(room))) =>
            RoomCreated(room, request, replyTo)
          case Success(RoomStore.CreateRoomResponse(Left(errorMessage))) =>
            RoomFailed(errorMessage, replyTo)
          case Failure(error) =>
            RoomFailed(error.getMessage, replyTo)
        }
      case JsError(errors) =>
        logger.info { s"Request JSON failed validation: $errors"}
        replyTo ! CreateRoomResponse(Left("Malformed request"))
    }
