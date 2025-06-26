package net.jcain.spelt.store

import net.jcain.spelt.models.User
import net.jcain.spelt.service.{Auth, Token}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

class SessionStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait ExistingUser {
    val existingPassword = "open-sesame"
    val existingUser: User = User("phredsmerd", Auth.hashPassword(existingPassword), "phredsmerd@example.com")

    // Create User in database.
    private val userStore = testKit.spawn(UserStore())
    private val userStoreProbe = testKit.createTestProbe[UserStore.Response]()
    userStore ! UserStore.CreateUser(
      existingUser.name,
      existingPassword,
      existingUser.email,
      userStoreProbe.ref
    )
    userStoreProbe.expectMessage(UserStore.CreateUserResponse(Right(existingUser.name)))
  }

  trait ExistingSession extends ExistingUser {
    private val sessionStoreRepo = testKit.spawn(SessionStore())
    private val sessionStoreProbe = testKit.createTestProbe[SessionStore.Response]()

    sessionStoreRepo !
      SessionStore.GetOrCreateSession(existingUser.name, None, None, sessionStoreProbe.ref)

    val existingSession: SessionStore.SessionCreated =
      sessionStoreProbe.expectMessageType[SessionStore.SessionCreated]
  }

  "GetOrCreateSession" when {
    "user has no previous session" should {
      "respond with SessionCreated" in new ExistingUser {
        // Send message and check response.
        private val repo = testKit.spawn(SessionStore())
        private val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.GetOrCreateSession(existingUser.name, None, None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            repo ! SessionStore.VerifyToken(token, probe.ref)
            probe.expectMessageType[SessionStore.TokenPassed]
        }
      }
    }

    "user has a previous session" should {
      "respond with SessionCreated with the same device ID and a new token" in new ExistingSession {
        private val repo = testKit.spawn(SessionStore())
        private val probe = testKit.createTestProbe[SessionStore.Response]()

        // Send message and check response.
        repo !
          SessionStore.GetOrCreateSession(existingUser.name, Some(existingSession.deviceId), None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            token should not equal existingSession.token
            deviceId should equal (existingSession.deviceId)
        }
      }
    }

    "previous session not found" should {
      "respond with SessionCreated with the same device ID and a new token" in {
        // Create User.
        val userRepo = testKit.spawn(UserStore())
        val userProbe = testKit.createTestProbe[UserStore.Response]()

        userRepo ! UserStore.CreateUser("phred", "secret", "phred@example.com", userProbe.ref)
        userProbe.expectMessageType[UserStore.Response] should matchPattern {
          case UserStore.CreateUserResponse(Right(_)) =>
        }

        // Send message and check response.
        val repo = testKit.spawn(SessionStore())
        val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.GetOrCreateSession("phred", Some("foo"), None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            repo ! SessionStore.VerifyToken(token, probe.ref)
            probe.expectMessageType[SessionStore.TokenPassed]
        }
      }
    }

    "user not found" should {
      "respond with UserNotFound" in {
        // Send message and check response.
        val repo = testKit.spawn(SessionStore())
        val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.GetOrCreateSession("phred", None, None, probe.ref)

        inside(probe.expectMessage(SessionStore.UserNotFound))
      }
    }
  }

  "DeleteSession" when {
    "session exists" should {
      "respond with SessionDeleted" in new ExistingSession {
        private val repo = testKit.spawn(SessionStore())
        private val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.DeleteSession(existingSession.ulid, probe.ref)

        probe.expectMessage(SessionStore.SessionDeleted)
      }
    }

    "session does not exist" should {
      "respond with SessionDeletionFailed" in {
        val repo = testKit.spawn(SessionStore())
        val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.DeleteSession("foobar", probe.ref)

        inside(probe.expectMessageType[SessionStore.SessionDeletionFailed]) {
          case SessionStore.SessionDeletionFailed(error) =>
            error should include("Session not found")
        }
      }
    }
  }

  "VerifyToken" when {
    "JWT is valid" when {
      "session exists" should {
        "respond with TokenPassed" in new ExistingSession {
          private val repo = testKit.spawn(SessionStore())
          private val probe = testKit.createTestProbe[SessionStore.Response]()

          repo ! SessionStore.VerifyToken(existingSession.token, probe.ref)

          probe.expectMessageType[SessionStore.TokenPassed]
        }
      }

      "session does not exist" should {
        "respond with TokenFailed" in {
          val repo = testKit.spawn(SessionStore())
          val probe = testKit.createTestProbe[SessionStore.Response]()
          val token = Token.generateAndSign(ULID.newULIDString)

          repo ! SessionStore.VerifyToken(token, probe.ref)

          inside(probe.expectMessageType[SessionStore.Response]) {
            case SessionStore.TokenFailed(error) =>
              error.getMessage should include ("Session not found")
          }
        }
      }
    }
  }
}
