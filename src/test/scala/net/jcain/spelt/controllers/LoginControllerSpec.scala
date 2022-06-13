package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.scalatest.Inside.inside
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class LoginControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)
  case class LoginResponse(access_token: String,
                           device_id: String,
                           user_id: String,
                           well_known: WellKnown)

  "GET /_matrix/client/v3/login" should {
    "return the available login types" in {
      val Some(response) = route(app, FakeRequest(GET, "/_matrix/client/v3/login"))

      status(response) mustBe OK
      contentAsString(response) must equal ("{\"flows\":[{\"type\":\"m.login.password\"}]}")
    }
  }

  "POST /_matrix/client/v3/login" when {
    "credentials are valid" should {
      "log in the user and return a JWT" in {
        val identifier = Json.obj(
          "type" -> "m.id.user",
          "user" -> "phredsmerd"
        )

        val payload = Json.obj(
          "identifier" -> identifier,
          "password" -> "foobar",
          "type" -> "m.login.password"
        )

        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/login").withBody(payload))

        status(response) mustBe OK

//        inside(JsonMethods.parse(body).extract[LoginResponse]) {
//          case LoginResponse(jwt, deviceId, userId, WellKnown(UrlSpec(homeUrl), UrlSpec(idUrl))) =>
//            deviceId shouldBe a [String]
//            userId should equal ("phredsmerd")
//            homeUrl should equal (Config.homeserverUrl)
//            idUrl should equal (Config.identityUrl)
//        }
      }
    }
  }
}
