package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.JsonMethods
import org.scalatra.test.scalatest.ScalatraWordSpec

class ConfigControllerSpec extends ScalatraWordSpec {
  case class Versions(versions: Seq[String])
  case class UrlSpec(base_url: String)
  case class WellKnown(`m.homeserver`: UrlSpec, `m.identity_server`: UrlSpec)

  addServlet(classOf[ConfigController], "/*")

  "GET /_matrix/client/versions" should {
    "return the versions JSON" in {
      get("/_matrix/client/versions") {
        status should equal (200)

        implicit val jsonFormats: Formats = DefaultFormats
        val versions = JsonMethods.parse(body).extract[Versions]

        /*
          For some reason, the following fails:
            JsonMethods.parse(body).extract[Versions] should equal(Versions(Seq("1.2")))
          Instead, test the individual values.
         */
        versions.versions should equal (Seq("1.2"))
      }
    }
  }

  "GET /.well-known/matrix/client" should {
    "return the homeserver and identity server URLs" in {
      get("/.well-known/matrix/client") {
        status should equal (200)

        implicit val jsonFormats: Formats = DefaultFormats
        val well_known = JsonMethods.parse(body).extract[WellKnown]

        /*
          For some reason, the following fails:
            well_known should equal(WellKnown(UrlSpec("foo"), UrlSpec("bar")))
          Instead, test the individual values.
         */
        well_known.`m.homeserver`.base_url should equal (Config.base.getString("server.base_url"))
        well_known.`m.identity_server`.base_url should equal (Config.base.getString("server.identity_server"))
      }
    }
  }
}
