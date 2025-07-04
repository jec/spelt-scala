package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.*
import play.api.libs.json.*
import play.api.test.*
import play.api.test.Helpers.*

class ConfigControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  "GET /_matrix/client/versions" should {
    "return the versions JSON" in {
      val Some(response) = route(app, FakeRequest(GET, "/_matrix/client/versions")): @unchecked

      status(response) mustBe OK

      val parsedBody = contentAsJson(response)

      (parsedBody \ "versions") must equal (JsDefined(JsArray(Seq(JsString("1.14")))))
    }
  }

  "GET /.well-known/matrix/client" should {
    "return the homeserver and identity server URLs" in {
      val Some(response) = route(app, FakeRequest(GET, "/.well-known/matrix/client")): @unchecked

      status(response) mustBe OK

      val parsedBody = contentAsJson(response)

      (parsedBody \ "m.homeserver" \ "base_url") must equal (JsDefined(JsString(s"https://${Config.homeserver}")))
      (parsedBody \ "m.identity_server" \ "base_url") must equal (JsDefined(JsString(Config.identityUrl)))
    }
  }
}
