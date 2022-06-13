package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.scalatest.Inside.inside
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.test._
import play.api.test.Helpers._

class LoginControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  case class WellKnown(`m.homeserver`: String, `m.identity_server`: String)
  case class LoginResponse(access_token: String,
                           device_id: String,
                           user_id: String,
                           well_known: WellKnown)

  implicit val wellKnownReads: Reads[WellKnown] = (
    (JsPath \ "m.homeserver" \ "base_url").read[String] and
      (JsPath \ "m.identity_server" \ "base_url").read[String]
  )(WellKnown.apply _)

  implicit val loginResponseReads: Reads[LoginResponse] = (
    (JsPath \ "access_token").read[String] and
      (JsPath \ "device_id").read[String] and
      (JsPath \ "user_id").read[String] and
      (JsPath \ "well_known").read[WellKnown]
  )(LoginResponse.apply _)

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

        inside(contentAsJson(response).validate[LoginResponse]) {
          case JsSuccess(LoginResponse(jwt, deviceId, userId, WellKnown(homeUrl, idUrl)), _) =>
            deviceId mustBe a [String]
            userId must equal ("phredsmerd")
            homeUrl must equal (Config.homeserverUrl)
            idUrl must equal (Config.identityUrl)
        }
      }
    }
  }
}
