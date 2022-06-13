package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class ConfigController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  /**
   * GET /_matrix/client/versions
   *
   * Returns an array of the supported Matrix versions
   */
  def versions() = Action { implicit request: Request[AnyContent] =>
    Ok(Map("versions" -> Config.versions))
  }

  /**
   * GET /.well-known/matrix/client
   *
   * Returns the "well-known" client information
   */
  def wellKnown() = Action { implicit request: Request[AnyContent] =>
    Ok(Config.wellKnown)
  }
}
