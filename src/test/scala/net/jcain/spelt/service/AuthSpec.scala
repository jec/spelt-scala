package net.jcain.spelt.service

import net.jcain.spelt.models.Config
import net.jcain.spelt.repo.UserRepo
import net.jcain.spelt.support.DatabaseRollback
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

class AuthSpec extends AnyWordSpecLike with Matchers with ValidUser with DatabaseRollback {
  "logIn()" when {
    "credentials are valid" should {
      "return an Auth.Success" in {
        val Right(identifier) = UserRepo.createUser("phredsmerd", "bar", "Phred Smerd", "phredsmerd@example.com")

        val identifierJson = render(
          ("type" -> "m.id.user") ~
            ("user" -> "phredsmerd")
        )

        val parsedParams = render(
          ("identifier" -> identifierJson) ~
            ("password" -> "foobar") ~
            ("type" -> "m.login.password")
        )

        inside(Auth.logIn(parsedParams)) {
          case Auth.Success("phredsmerd", jwt, deviceId) =>
            UUID.fromString(deviceId) shouldBe a [UUID]

            inside(Token.verify(jwt)) {
              case Right(decodedJwt) =>
                decodedJwt.getIssuer should equal (Config.jwtIssuer)
                // TODO: Token subject should indicate the Session.
            }
        }
      }
    }
  }

  "hashPassword()" should {
    "return a hashed password" in {
      val plainPassword = "phredsmerd"

      Auth.hashPassword(plainPassword) shouldNot equal (plainPassword)
    }
  }

  "passwordMatches()" when {
    "password is valid" should {
      "return true" in {
        val plainPassword = "phredsmerd"

        Auth.passwordMatches(Auth.hashPassword(plainPassword), plainPassword) should equal (true)
      }
    }

    "password is invalid" should {
      "return true" in {
        Auth.passwordMatches(Auth.hashPassword("phredsmerd"), "phredsmerdx") should equal (false)
      }
    }
  }
}
