package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.scalatest.Inside.inside
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class ConfigControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  case class Versions(versions: Seq[String])
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)


  "GET /_matrix/client/versions" should {
    "return the versions JSON" in {
      val Some(response) = route(app, FakeRequest(GET, "/_matrix/client/versions"))

      status(response) mustBe OK
//      contentAsJson(response)
//
//      inside(JsonMethods.parse(body).extract[Versions]) {
//        case Versions(versions) =>
//          versions should equal (Seq("1.2"))
//      }
    }
  }

  "GET /.well-known/matrix/client" should {
    "return the homeserver and identity server URLs" in {
      val Some(response) = route(app, FakeRequest(GET, "/.well-known/matrix/client"))

      status(response) mustBe OK

//      inside(JsonMethods.parse(body).extract[WellKnown]) {
//        case WellKnown(UrlSpec(homeUrl), UrlSpec(idUrl)) =>
//          homeUrl should equal (Config.homeserverUrl)
//          idUrl should equal (Config.identityUrl)
//      }
    }
  }
}
