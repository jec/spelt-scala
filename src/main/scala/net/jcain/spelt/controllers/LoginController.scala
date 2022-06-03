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
      case Auth.Success(userId, jwt, deviceId) =>
        Ok(Map(
          "access_token" -> jwt,
          "device_id" -> deviceId,
          "user_id" -> userId,
          "well_known" -> Config.wellKnown
        ))

      case Auth.Unauthenticated(message) =>
        Unauthorized(Map("error_message" -> message))

      case Auth.Failure(message) =>
        BadRequest(Map("error_message" -> message))
    }
  }
}
