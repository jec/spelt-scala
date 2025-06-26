package net.jcain.spelt.service

import net.jcain.spelt.models.{Session, User}
import net.jcain.spelt.store.{SessionStore, UserStore}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import wvlet.airframe.ulid.ULID

class AuthSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait ExistingUser {
    val existingPassword = "open-sesame"
    val existingUser: User = User("phredsmerd", Auth.hashPassword(existingPassword), "phredsmerd@example.com")
  }

  trait ExistingSession {
    val existingSession: Session = Session(ULID.newULIDString, "foo.bar.baz")
  }

  trait LoginRequestParams extends ExistingUser {
    val requestDeviceId: String = ULID.newULIDString
    val requestDeviceName = "iDevice 123 Max Pro Extreme"

    val identifierJson: JsObject = Json.obj(
      "type" -> "m.id.user",
      "user" -> existingUser.name
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
            username shouldEqual existingUser.name
            replyTo ! UserStore.GetUserResponse(Right(Some(existingUser)))
        }

        // Create a ULID and JWT that the SessionStore would create upon success.
        private val sessionUlid: String = ULID.newULIDString
        private val token = Token.generateAndSign(sessionUlid)

        // Expect SessionStore to receive GetOrCreateSession; respond with SessionCreated.
        inside(sessionStoreProbe.expectMessageType[SessionStore.Request]) {
          case SessionStore.GetOrCreateSession(username, deviceId, deviceName, replyTo) =>
            username shouldEqual existingUser.name
            deviceId shouldEqual Some(requestDeviceId)
            deviceName shouldEqual Some(requestDeviceName)

            replyTo ! SessionStore.SessionCreated(sessionUlid, token, deviceId.get)
        }

        // Expect Auth to respond with LoginSucceeded.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LoginSucceeded(username, receivedToken, deviceId) =>
            username shouldEqual existingUser.name
            receivedToken shouldEqual token
            deviceId shouldEqual requestDeviceId
        }
      }
    }
  }

  "LogOut" when {
    "session exists" should {
      "respond with LogoutSucceeded" in new ExistingSession {
        private val userStoreProbe = testKit.createTestProbe[UserStore.Request]()
        private val sessionStoreProbe = testKit.createTestProbe[SessionStore.Request]()
        private val auth = testKit.spawn(Auth(userStoreProbe.ref, sessionStoreProbe.ref))

        // Send LogOut message to Auth.
        private val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOut(existingSession.ulid, probe.ref)

        // Expect SessionStore to receive DeleteSession; respond with SessionDeleted.
        inside(sessionStoreProbe.expectMessageType[SessionStore.Request]) {
          case SessionStore.DeleteSession(sessionId, replyTo) =>
            sessionId shouldEqual existingSession.ulid

            replyTo ! SessionStore.SessionDeleted
        }

        // Expect Auth to respond with LogoutSucceeded.
        probe.expectMessage(Auth.LogoutSucceeded)
      }
    }

    "session does not exist" should {
      "respond with LogoutFailed" in {
        val userStoreProbe = testKit.createTestProbe[UserStore.Request]()
        val sessionStoreProbe = testKit.createTestProbe[SessionStore.Request]()
        val auth = testKit.spawn(Auth(userStoreProbe.ref, sessionStoreProbe.ref))

        // Send LogOut message to Auth.
        val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOut("foobar", probe.ref)

        // Expect SessionStore to receive DeleteSession; respond with SessionDeletionFailed.
        inside(sessionStoreProbe.expectMessageType[SessionStore.Request]) {
          case SessionStore.DeleteSession(sessionId, replyTo) =>
            sessionId shouldEqual "foobar"

            replyTo ! SessionStore.SessionDeletionFailed("Session not found")
        }

        // Expect Auth to respond with LogoutSucceeded.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LogoutFailed(message) =>
            message should include ("Session not found")
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
