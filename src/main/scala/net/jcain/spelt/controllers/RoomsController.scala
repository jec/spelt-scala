package net.jcain.spelt.controllers

import net.jcain.spelt.service.AuthenticatedAction
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.Future

/**
 * POST /_matrix/client/v3/createRoom
 *
 * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3createroom
 *
 * @param authenticatedAction  provides authenticated User, Session and Device [injected]
 * @param controllerComponents required by `BaseController` [injected]
 */
class RoomsController @Inject() (
  authenticatedAction: AuthenticatedAction,
  val controllerComponents: ControllerComponents
) extends BaseController {
  def createRoom(): Action[JsValue] = authenticatedAction.async { (request: AuthenticatedAction.AuthenticatedRequest[JsValue]) =>
    // Room
    // Event m.room.create
    // Event m.room.member
    // Event m.room.power_levels
    // Event m.room.canonical_alias if `room_alias_name`
    // Events in `preset`
    // Events in `initial_state`
    // Event m.room_name if `name`
    // Event m.room.topic if `topic`
    // Events from `invite` and `invite_3pid`
    Future.successful(Ok(Json.obj()))
  }
}
