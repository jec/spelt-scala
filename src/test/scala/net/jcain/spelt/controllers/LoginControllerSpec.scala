package net.jcain.spelt.controllers

import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.JsonMethods
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, render}
import org.scalatest.Inside.inside
import org.scalatra.test.scalatest.ScalatraWordSpec

class LoginControllerSpec extends ScalatraWordSpec {
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)
  case class LoginResponse(
                          access_token: String,
                          device_id: String,
                          user_id: String,
                          well_known: WellKnown
                          )

  addServlet(classOf[LoginController], "/_matrix/client/v3/*")

  "GET /_matrix/client/v3/login" should {
    "return the available login types" in {
      get("_matrix/client/v3/login") {
        status should equal (200)
        body should equal ("{\"flows\":[{\"type\":\"m.login.password\"}]}")
      }
    }
  }

  "POST /_matrix/client/v3/login" when {
    "credentials are valid" should {
      "log in a user and return a JWT" in {
        val identifier = render(
          ("type" -> "m.id.user") ~
          ("user" -> "phredsmerd")
        )

        val payload = compact(render(
          ("identifier" -> identifier) ~
          ("password" -> "foobar") ~
          ("type" -> "m.login.password")
        ))

        post("_matrix/client/v3/login", payload) {
          implicit val jsonFormats: Formats = DefaultFormats

          status should equal (200)

          inside(JsonMethods.parse(body).extract[LoginResponse]) {
            case LoginResponse(jwt, device_id, user_id, WellKnown(UrlSpec(home_url), UrlSpec(id_url))) =>
              device_id should equal ("foo")
              user_id should equal ("bar")
              home_url should equal ("baz")
              id_url should equal ("phred")
          }
        }
      }
    }
  }
}
