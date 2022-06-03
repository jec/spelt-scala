package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport

class ConfigController extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/_matrix/client/versions") {
    Ok(Map("versions" -> Config.versions))
  }

  get("/.well-known/matrix/client") {
    Ok(Config.wellKnown)
  }
}
