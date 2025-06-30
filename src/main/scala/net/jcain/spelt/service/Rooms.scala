package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.json.Reads.requests.createRoomRequest
import net.jcain.spelt.models.Room
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.RoomStore
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, TypedActorContext}
import play.api.libs.concurrent.ActorModule
import play.api.libs.json.JsValue
import play.api.libs.json.*
import play.api.libs.json.Reads.*

object Rooms extends ActorModule:
  type Message = Request

  // Actor messages
  sealed trait Request
  final case class CreateRoom(params: JsValue, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateRoomResponse(roomOrError: Either[String, Room]) extends Response

  private type RequestAndResponse = Request | RoomStore.Response

  @Provides
  def apply(
    roomStore: ActorRef[RoomStore.Request]
  ): Behavior[Request] =
    Behaviors.setup[RequestAndResponse] { context =>
      Behaviors.receiveMessage[RequestAndResponse] {
        case CreateRoom(params, replyTo) =>
          createRoom(params, roomStore, replyTo, context.self)
          Behaviors.same

        case RoomStore.CreateRoomResponse(Right(room)) =>
          Behaviors.same

        case RoomStore.CreateRoomResponse(Left(errorMessage)) =>
          Behaviors.same
      }
    }.narrow

  private def createRoom(params: JsValue,
                         roomStore: ActorRef[RoomStore.Request],
                         replyTo: ActorRef[Response],
                         self: ActorRef[RequestAndResponse]): Unit =
    params.validate[CreateRoomRequest] match {
      case JsSuccess(request, _) =>
        roomStore ! RoomStore.CreateRoom(request, self)
      case JsError(errors) =>
        println(errors)
        replyTo ! CreateRoomResponse(Left("Malformed request"))
    }
