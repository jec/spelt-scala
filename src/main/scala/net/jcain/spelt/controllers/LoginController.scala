package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import net.jcain.spelt.service.Auth
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class LoginController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def show() = Action { implicit request: Request[AnyContent] =>
    Ok("woo hoo")
//    Ok(Map("flows" -> Seq(Map("type" -> "m.login.password"))))
  }

  def create() = Action { implicit request: Request[AnyContent] =>
    Ok("good deal")
//    Auth.logIn(request.body.asJson.get) match {
//      case Auth.Success(userId, jwt, deviceId) =>
//        Ok(Map(
//          "access_token" -> jwt,
//          "device_id" -> deviceId,
//          "user_id" -> userId,
//          "well_known" -> Config.wellKnown
//        ))
//
//      case Auth.Unauthenticated(message) =>
//        Unauthorized(Map("error_message" -> message))
//
//      case Auth.Failure(message) =>
//        BadRequest(Map("error_message" -> message))
//    }
  }
}
