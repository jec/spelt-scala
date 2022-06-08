package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.JsonMethods
import org.scalatest.Inside.inside
import org.scalatra.test.scalatest.ScalatraWordSpec

class ConfigControllerSpec extends ScalatraWordSpec {
  case class Versions(versions: Seq[String])
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)

  addServlet(classOf[ConfigController], "/*")

  "GET /_matrix/client/versions" should {
    "return the versions JSON" in {
      get("/_matrix/client/versions") {
        implicit val jsonFormats: Formats = DefaultFormats

        status should equal (200)

        inside(JsonMethods.parse(body).extract[Versions]) {
          case Versions(versions) =>
            versions should equal (Seq("1.2"))
        }
      }
    }
  }

  "GET /.well-known/matrix/client" should {
    "return the homeserver and identity server URLs" in {
      get("/.well-known/matrix/client") {
        implicit val jsonFormats: Formats = DefaultFormats

        status should equal (200)

        inside(JsonMethods.parse(body).extract[WellKnown]) {
          case WellKnown(UrlSpec(homeUrl), UrlSpec(idUrl)) =>
            homeUrl should equal (Config.homeserverUrl)
            idUrl should equal (Config.identityUrl)
        }
      }
    }
  }
}
