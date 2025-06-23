package net.jcain.spelt.controllers

import net.jcain.spelt.service.Auth
import org.apache.pekko.actor.typed.ActorRef
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*

import javax.inject.{Inject, Named, Singleton}

@Singleton
class LoginController @Inject() (
  @Named("AuthActor") authRef: ActorRef[Auth.Request],
  val controllerComponents: ControllerComponents
) extends BaseController {
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
    Ok(Json.obj("flows" -> Json.arr(Json.obj("type" -> "m.login.password"))))
  }

  /**
   * POST /_matrix/client/v3/login
   *
   * Authenticates a user and, if successful, responds with a token
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3login
   */
  def logIn(): Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] =>
    authRef.ask(ref => Auth.LogIn(request.body.asJson.get, ref)) {
      case Success(Auth.LoginSucceeded(identifier, token, deviceId)) =>
        Ok(Json.obj(
          "access_token" -> token,
          "device_id" -> deviceId,
          "user_id" -> identifier,
          "well_known" -> Config.wellKnown
        ))

      case Success(Auth.LoginFailed(message)) =>
        Unauthorized(Json.obj("error_message" -> message))

      case Failure(error) =>
        InternalError(Json.obj("error_message" -> message))
    }
  }
}
