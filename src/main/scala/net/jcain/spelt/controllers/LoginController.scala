package net.jcain.spelt.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*

import javax.inject.{Inject, Singleton}

@Singleton
class LoginController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
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
//    auth ! Auth.LogIn(request.body.asJson.get, self.ref)
//    Auth.logIn(request.body.asJson.get) match {
//      case Auth.Success(userId, jwt, deviceId) =>
//        Ok(Json.obj(
//          "access_token" -> jwt,
//          "device_id" -> deviceId,
//          "user_id" -> userId,
//          "well_known" -> Config.wellKnown
//        ))
//
//      case Auth.Unauthenticated(message) =>
//        Unauthorized(Json.obj("error_message" -> message))
//
//      case Auth.Failure(message) =>
//        BadRequest(Json.obj("error_message" -> message))
//    }
    BadRequest(Json.obj("error_message" -> "oops"))
  }
}
