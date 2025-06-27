package net.jcain.spelt.controllers

import net.jcain.spelt.controllers.LoginController.supportedLoginFlows
import net.jcain.spelt.models.Config
import net.jcain.spelt.service.{Auth, AuthenticatedAction}
import net.jcain.spelt.store.SessionStore
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

object LoginController:
  /**
   * The Matrix login flows supported by the app
   */
  val supportedLoginFlows: JsObject = Json.obj(
    "flows" -> Json.arr(
      Json.obj("type" -> "m.login.password")
    )
  )

/**
 * Implements endpoints related to user login
 *
 * This is not a Singleton so that it receives the current Auth actor reference each time.
 *
 * @param authRef              reference to Auth actor [injected]
 * @param controllerComponents required by `BaseController` [injected]
 * @param xc                   required for interacting with the Actor System
 * @param sch                  required for interacting with the Actor System
 */
class LoginController @Inject() (
  authRef: ActorRef[Auth.Request],
  sessionStoreRef: ActorRef[SessionStore.Request],
  authenticatedAction: AuthenticatedAction,
  val controllerComponents: ControllerComponents
)(
  implicit xc: ExecutionContext,
  sch: Scheduler
) extends BaseController {
  implicit val timeout: Timeout = 5.seconds

  /**
   * GET /_matrix/client/v3/login
   *
   * Returns the supported login flows
   *
   * Currently, the only supported login flow is "m.login.password".
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#get_matrixclientv3login
   */
  def loginTypes(): Action[AnyContent] = Action {
    Ok(supportedLoginFlows)
  }

  /**
   * POST /_matrix/client/v3/login
   *
   * Authenticates a user and, if successful, responds with a token
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3login
   */
  def logIn(): Action[JsValue] = Action.async(parse.json) { request =>
    val body = request.body

    authRef.ask(ref => Auth.LogIn(body, ref))
      .map {
        case Auth.LoginSucceeded(name, token, deviceId) =>
          Ok(Json.obj(
            "access_token" -> JsString(token),
            "device_id" -> JsString(deviceId),
            "user_id" -> JsString(s"@$name:${Config.homeserver}"),
            "well_known" -> Config.wellKnown
          ))

        case Auth.LoginFailed(message) =>
          Unauthorized(Json.obj("error_message" -> JsString(message)))
      }
  }

  /**
   * POST /_matrix/client/v3/logout
   *
   * [Authenticated] Deletes the current Session
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3logout
   */
  def logOut(): Action[JsValue] = authenticatedAction.async { (request: AuthenticatedAction.AuthenticatedRequest[JsValue]) =>
    authRef.ask(ref => Auth.LogOut(request.currentSession.ulid, ref))
      .map {
        case Auth.LogoutSucceeded =>
          Ok(Json.obj())
        case Auth.LogoutFailed(message) =>
          InternalServerError(Json.obj("error_message" -> message))
      }
  }

  /**
   * POST /_matrix/client/v3/logout/all
   *
   * [Authenticated] Deletes all Sessions and Devices of the current User
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3logoutall
   */
  def logOutAll(): Action[JsValue] = authenticatedAction.async { (request: AuthenticatedAction.AuthenticatedRequest[JsValue]) =>
    authRef.ask(ref => Auth.LogOutAll(request.currentUser.name, ref))
      .map {
        case Auth.LogoutAllSucceeded =>
          Ok(Json.obj())
        case Auth.LogoutAllFailed(message) =>
          InternalServerError(Json.obj("error_message" -> message))
      }
  }
}
