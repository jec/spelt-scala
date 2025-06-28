package net.jcain.spelt.service

import net.jcain.spelt.models.{Session, User}
import net.jcain.spelt.support.{DatabaseRollback, MockSessionStore, MockUserStore}
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import wvlet.airframe.ulid.ULID

/**
 * Tests the message interface of the `Auth` actor
 *
 * These tests use mocks for the actors with which `Auth` communicates and therefore do not touch
 * the database.
 */
class AuthSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait ExistingUser {
    val existingPassword = "open-sesame"
    val existingUser: User = User("phredsmerd", Auth.hashPassword(existingPassword), "phredsmerd@example.com")
  }

  trait ExistingSession extends ExistingUser {
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
        // Create a Session object with which the SessionStore would respond upon success.
        private val sessionUlid: String = ULID.newULIDString
        private val newSession = Session(sessionUlid, Token.generateAndSign(sessionUlid))

        // Mock the UserStore to respond to `GetUser` with `existingUser`.
        private val userStore = testKit.spawn(MockUserStore(existingUser))
        // Mock the SessionStore to respond to `GetOrCreateSession` with `newSession`.
        private val sessionStore = testKit.spawn(MockSessionStore(newSession))
        private val auth = testKit.spawn(Auth(userStore.ref, sessionStore.ref))

        // Send LogIn message to Auth.
        private val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogIn(parsedParams, "1.2.3.4", probe.ref)

        // Expect Auth to respond with LoginSucceeded.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LoginSucceeded(username, receivedToken, deviceId) =>
            username shouldEqual existingUser.name
            receivedToken shouldEqual newSession.token
            deviceId shouldEqual requestDeviceId
        }
      }
    }
  }

  "LogOut" when {
    "Session exists" should {
      "respond with LogoutSucceeded" in new ExistingSession {
        // Mock the UserStore. It should receive no messages.
        private val userStore = testKit.spawn(MockUserStore())
        // Mock the SessionStore to respond to `DeleteSession` with `SessionDeleted`.
        private val sessionStore = testKit.spawn(MockSessionStore(existingSession))
        private val auth = testKit.spawn(Auth(userStore.ref, sessionStore.ref))

        // Send LogOut message to Auth.
        private val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOut(existingSession.ulid, probe.ref)

        // Expect Auth to respond with LogoutSucceeded.
        probe.expectMessage(Auth.LogoutSucceeded)
      }
    }

    "Session does not exist" should {
      "respond with LogoutFailed" in {
        // Mock the UserStore. It should receive no messages.
        val userStore = testKit.spawn(MockUserStore())
        // Mock the SessionStore to respond to `DeleteSession` with `SessionDeletionFailed`.
        val sessionStore = testKit.spawn(MockSessionStore())
        val auth = testKit.spawn(Auth(userStore.ref, sessionStore.ref))

        // Send LogOut message to Auth.
        val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOut("foobar", probe.ref)

        // Expect Auth to respond with LogoutFailed.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LogoutFailed(message) =>
            message shouldEqual "Session not found"
        }
      }
    }
  }

  "LogOutAll" when {
    "User exists and Sessions exist" should {
      "respond with LogoutAllSucceeded" in new ExistingSession {
        // Mock the UserStore. It should receive no messages.
        private val userStore = testKit.spawn(MockUserStore())
        // Mock the SessionStore to respond to `DeleteAllSessions` with `AllSessionsDeleted`.
        private val sessionStore = testKit.spawn(MockSessionStore(existingSession))
        private val auth = testKit.spawn(Auth(userStore.ref, sessionStore.ref))

        // Send LogOutAll message to Auth.
        private val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOutAll(existingUser.name, probe.ref)

        // Expect Auth to respond with LogoutAllSucceeded.
        probe.expectMessage(Auth.LogoutAllSucceeded)
      }
    }

    "User does not exist" should {
      "respond with LogoutAllFailed" in {
        // Mock the UserStore. It should receive no messages.
        val userStore = testKit.spawn(MockUserStore())
        // Mock the SessionStore to respond to `DeleteAllSession` with `AllSessionsDeletionFailed`.
        val sessionStore = testKit.spawn(MockSessionStore())
        val auth = testKit.spawn(Auth(userStore.ref, sessionStore.ref))

        // Send LogOutAll message to Auth.
        val probe = testKit.createTestProbe[Auth.Response]()
        auth ! Auth.LogOutAll("foobar", probe.ref)

        // Expect Auth to respond with LogoutAllFailed.
        inside(probe.expectMessageType[Auth.Response]) {
          case Auth.LogoutAllFailed(message) =>
            message shouldEqual "User not found"
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
