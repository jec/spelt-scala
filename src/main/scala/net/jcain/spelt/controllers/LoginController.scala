package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import net.jcain.spelt.service.Auth
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class LoginController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  /**
   * GET /_matrix/client/v3/login
   *
   * Returns the supported login types
   */
  def show(): Action[AnyContent] = Action {
    Ok(Json.obj("flows" -> Seq(Map("type" -> "m.login.password"))))
  }

  /**
   * POST /_matrix/client/v3/login
   *
   * Creates a Session and returns a JWT for authenticating subsequent API
   * requests
   */
  def create(): Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] =>
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
