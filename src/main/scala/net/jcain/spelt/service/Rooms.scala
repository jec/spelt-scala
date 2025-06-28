package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.json.Reads.requests.createRoomRequest
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.RoomStore
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import play.api.libs.concurrent.ActorModule
import play.api.libs.json.JsValue
import play.api.libs.json.*
import play.api.libs.json.Reads.*

object Rooms extends ActorModule:
  type Message = Request

  // Actor messages
  sealed trait Request
  final case class CreateRoom(params: JsValue) extends Request

  sealed trait Response

  @Provides
  def apply(
    roomStore: ActorRef[RoomStore.Request]
  ): Behavior[Request] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case CreateRoom(params) =>
          createRoom(params)
          Behaviors.same
      }
    }

  private def createRoom(params: JsValue): Unit =
    params.validate[CreateRoomRequest] match {
      case JsSuccess(request, _) =>
      case _ =>
    }
    // Room
    ()
