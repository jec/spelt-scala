package net.jcain.spelt.service

import net.jcain.spelt.models.User
import net.jcain.spelt.store.{SessionStore, UserStore}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}

import java.util.UUID

class AuthSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait ExistingUser {
    val existingPassword = "open-sesame"
    val existingUser: User = User("phredsmerd", Auth.hashPassword(existingPassword), "phredsmerd@example.com")
  }

  trait LoginRequestParams extends ExistingUser {
    val requestDeviceId: String = UUID.randomUUID.toString
    val requestDeviceName = "iDevice 123 Max Pro Extreme"

    val identifierJson: JsObject = Json.obj(
      "type" -> "m.id.user",
      "user" -> existingUser.identifier
    )

    val parsedParams: JsObject = Json.obj(
      "identifier" -> identifierJson,
      "password" -> existingPassword,
      "type" -> "m.login.password",
      "device_id" -> requestDeviceId,
      "initial_device_display_name" -> requestDeviceName
    )
  }

  "LogIn" when {
    "credentials are valid" should {
      "respond with Auth.Succeeded" in new LoginRequestParams {
        private val userStoreProbe = testKit.createTestProbe[UserStore.Request]()
        private val sessionStoreProbe = testKit.createTestProbe[SessionStore.Request]()
        private val auth = testKit.spawn(Auth(userStoreProbe.ref, sessionStoreProbe.ref))

        // Send LogIn message to Auth.
        private val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogIn(parsedParams, probe.ref)

        // Expect UserStore to receive GetUser; respond with CreateUserResponse.
        inside(userStoreProbe.expectMessageType[UserStore.Request]) {
          case UserStore.GetUser(username, replyTo) =>
            username shouldEqual existingUser.identifier
            replyTo ! UserStore.GetUserResponse(Right(Some(existingUser)))
        }

        // Create a UUID and JWT that the SessionStore would create upon success.
        private val sessionUuid: String = UUID.randomUUID.toString
        private val token = Token.generateAndSign(sessionUuid)

        // Expect SessionStore to receive GetOrCreateSession; respond with SessionCreated.
        inside(sessionStoreProbe.expectMessageType[SessionStore.Request]) {
          case SessionStore.GetOrCreateSession(username, deviceId, deviceName, replyTo) =>
            username shouldEqual existingUser.identifier
            deviceId shouldEqual Some(requestDeviceId)
            deviceName shouldEqual Some(requestDeviceName)

            replyTo ! SessionStore.SessionCreated(token, deviceId.get)
        }

        // Expect Auth to respond with LoginSucceeded.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LoginSucceeded(username, receivedToken, deviceId) =>
            username shouldEqual existingUser.identifier
            receivedToken shouldEqual token
            deviceId shouldEqual requestDeviceId
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
