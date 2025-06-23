package net.jcain.spelt.store

import net.jcain.spelt.service.Token
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

class SessionStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "GetOrCreateSession" when {
    "User has no previous session" should {
      "respond with SessionCreated" in {
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

        repo ! SessionStore.GetOrCreateSession("phred", None, None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(token, deviceId) =>
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            repo ! SessionStore.ValidateToken(token, probe.ref)
            probe.expectMessage(SessionStore.TokenValid)
        }
      }
    }

    "User has a previous session" should {
      "respond with SessionCreated with the same device ID and a new token" in {
        // Create User.
        val userRepo = testKit.spawn(UserStore())
        val userProbe = testKit.createTestProbe[UserStore.Response]()

        userRepo ! UserStore.CreateUser("phred", "secret", "phred@example.com", userProbe.ref)
        userProbe.expectMessageType[UserStore.Response] should matchPattern {
          case UserStore.CreateUserResponse(Right(_)) =>
        }

        // Send message to create Session.
        val repo = testKit.spawn(SessionStore())
        val probe = testKit.createTestProbe[SessionStore.Response]()
        repo ! SessionStore.GetOrCreateSession("phred", None, None, probe.ref)
        val response = probe.expectMessageType[SessionStore.SessionCreated]

        // Send message again and check response.
        repo ! SessionStore.GetOrCreateSession("phred", Some(response.deviceId), None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(token, deviceId) =>
            token shouldEqual response.token
            deviceId shouldEqual response.deviceId
        }
      }
    }

    "Previous session not found" should {
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
          case SessionStore.SessionCreated(token, deviceId) =>
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            repo ! SessionStore.ValidateToken(token, probe.ref)
            probe.expectMessage(SessionStore.TokenValid)
        }
      }
    }

    "User not found" should {
      "respond with UserNotFound" in {
        // Send message and check response.
        val repo = testKit.spawn(SessionStore())
        val probe = testKit.createTestProbe[SessionStore.Response]()

        repo ! SessionStore.GetOrCreateSession("phred", None, None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.UserNotFound =>
        }
      }
    }
  }

  "ValidateToken" when {
    "JWT is valid" when {
      "Session does not exist" should {
        "respond with Invalid" in {
          val repo = testKit.spawn(SessionStore())
          val probe = testKit.createTestProbe[SessionStore.Response]()
          val token = Token.generateAndSign(ULID.newULIDString)

          repo ! SessionStore.ValidateToken(token, probe.ref)

          inside(probe.expectMessageType[SessionStore.Response]) {
            case SessionStore.TokenInvalid(error) =>
              error.getMessage should include ("Session not found")
          }
        }
      }
    }
  }
}
