package net.jcain.spelt.service

import net.jcain.spelt.models.Config
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, render}
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

trait ValidUser {
  val userUuid = {

  }
}

class AuthSpec extends AnyWordSpecLike with Matchers with ValidUser {
  "logIn()" when {
    "credentials are valid" in {
      val identifier = render(
        ("type" -> "m.id.user") ~
          ("user" -> "phredsmerd")
      )

      val parsedParams = render(
        ("identifier" -> identifier) ~
          ("password" -> "foobar") ~
          ("type" -> "m.login.password")
      )

      inside(Auth.logIn(parsedParams)) {
        case Auth.Success("phredsmerd", jwt, deviceId) =>
          UUID.fromString(deviceId) shouldBe a [UUID]

          inside(Token.verify(jwt)) {
            case Right(decodedJwt) =>
              decodedJwt.getSubject should equal ("phredsmerd")
              decodedJwt.getIssuer should equal (Config.jwtIssuer)
          }
      }
    }
  }
}
