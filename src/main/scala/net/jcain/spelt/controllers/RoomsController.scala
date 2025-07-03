package net.jcain.spelt.controllers

import net.jcain.spelt.service.{Auth, AuthenticatedAction, Rooms}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
 * POST /_matrix/client/v3/createRoom
 *
 * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3createroom
 *
 * @param authenticatedAction  provides authenticated User, Session and Device [injected]
 * @param roomsRef             reference to Rooms actor [injected]
 * @param controllerComponents required by `BaseController` [injected]
 * @param xc                   required for interacting with the Actor System
 * @param sch                  required for interacting with the Actor System
 */
class RoomsController @Inject()(authenticatedAction: AuthenticatedAction,
                                roomsRef: ActorRef[Rooms.Request],
                                val controllerComponents: ControllerComponents
                               )(implicit xc: ExecutionContext,
                                 sch: Scheduler) extends BaseController {
  implicit val timeout: Timeout = 5.seconds

  def createRoom(): Action[JsValue] =
    authenticatedAction.async { (request: AuthenticatedAction.AuthenticatedRequest[JsValue]) =>
      roomsRef.ask(ref => Rooms.CreateRoom(request.body, request.currentUser, ref))
        .map {
          case Rooms.CreateRoomResponse(Right(_)) =>
            Ok(Json.obj())
          case Auth.LogoutAllFailed(message) =>
            InternalServerError(Json.obj("error_message" -> message))
        }
    }
}
