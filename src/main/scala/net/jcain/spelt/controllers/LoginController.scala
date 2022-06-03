package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import net.jcain.spelt.service.Auth
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{BadRequest, Ok, ScalatraServlet, Unauthorized}
import org.scalatra.json.JacksonJsonSupport

class LoginController extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/login") {
    Ok(Map("flows" -> Seq(Map("type" -> "m.login.password"))))
  }

  post("/login") {
    Auth.logIn(parsedBody) match {
      case Auth.Success(user) =>
        Ok(Map(
          "access_token" -> "foo",
          "device_id" -> "baz",
          "user_id" -> "bar",
          "well_known" -> Map(
            "m.homeserver" -> Map("base_url" -> Config.base.getString("server.base_url")),
            "m.identity_server" -> Map("base_url" -> Config.base.getString("server.identity_server"))
          )
        ))

      case Auth.Unauthenticated(message) =>
        Unauthorized(Map("error_message" -> message))

      case Auth.Failure(message) =>
        BadRequest(Map("error_message" -> message))
    }
  }
}
