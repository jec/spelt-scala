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
    Ok(Map(
      "m.homeserver" -> Map("base_url" -> Config.base.getString("server.base_url")),
      "m.identity_server" -> Map("base_url" -> Config.base.getString("server.identity_server"))
    ))
  }
}
