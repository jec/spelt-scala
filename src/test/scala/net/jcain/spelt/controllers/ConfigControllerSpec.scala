package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.scalatest.Inside.inside
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

class ConfigControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)


  "GET /_matrix/client/versions" should {
    "return the versions JSON" in {
      val Some(response) = route(app, FakeRequest(GET, "/_matrix/client/versions"))

      status(response) mustBe OK

      val parsedBody = contentAsJson(response)

      (parsedBody \ "versions") must equal (JsDefined(JsArray(Seq(JsString("1.2")))))
    }
  }

  "GET /.well-known/matrix/client" should {
    "return the homeserver and identity server URLs" in {
      val Some(response) = route(app, FakeRequest(GET, "/.well-known/matrix/client"))

      status(response) mustBe OK

      val parsedBody = contentAsJson(response)

      (parsedBody \ "m.homeserver" \ "base_url") must equal (JsDefined(JsString(Config.homeserverUrl)))
      (parsedBody \ "m.identity_server" \ "base_url") must equal (JsDefined(JsString(Config.identityUrl)))
    }
  }
}
