package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class ConfigController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  /**
   * GET /_matrix/client/versions
   *
   * Returns an array of the supported Matrix versions
   */
  def versions(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(Json.obj("versions" -> Config.versions))
  }

  /**
   * GET /.well-known/matrix/client
   *
   * Returns the "well-known" client information
   */
  def wellKnown(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(Config.wellKnown)
  }
}
