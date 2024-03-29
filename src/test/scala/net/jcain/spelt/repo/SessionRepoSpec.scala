package net.jcain.spelt.repo

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import net.jcain.spelt.models.Config
import net.jcain.spelt.service.Token
import net.jcain.spelt.support.DatabaseRollback
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class SessionRepoSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "GetOrCreateSession" when {
    "User has no previous session" should {
      "respond with SessionCreated" in {
        // Create User.
        val userRepo = testKit.spawn(UserRepo())
        val userProbe = testKit.createTestProbe[UserRepo.Response]()

        userRepo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", userProbe.ref)
        userProbe.expectMessageType[UserRepo.Response] should matchPattern {
          case UserRepo.CreateUserResponse(Right(_)) =>
        }

        // Send message and check response.
        val repo = testKit.spawn(SessionRepo())
        val probe = testKit.createTestProbe[SessionRepo.Response]()

        repo ! SessionRepo.GetOrCreateSession("phred", None, None, probe.ref)

        inside(probe.expectMessageType[SessionRepo.Response]) {
          case SessionRepo.SessionCreated(token, deviceId) =>
            UUID.fromString(deviceId) shouldBe a [UUID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            repo ! SessionRepo.ValidateToken(token, probe.ref)
            probe.expectMessage(SessionRepo.Valid)
        }
      }
    }

    "User has a previous session" should {
      "respond with SessionCreated with the same device ID and a new token" in {
        // Create User.
        val userRepo = testKit.spawn(UserRepo())
        val userProbe = testKit.createTestProbe[UserRepo.Response]()

        userRepo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", userProbe.ref)
        userProbe.expectMessageType[UserRepo.Response] should matchPattern {
          case UserRepo.CreateUserResponse(Right(_)) =>
        }

        // Send message to create Session.
        val repo = testKit.spawn(SessionRepo())
        val probe = testKit.createTestProbe[SessionRepo.Response]()
        repo ! SessionRepo.GetOrCreateSession("phred", None, None, probe.ref)
        val response = probe.expectMessageType[SessionRepo.SessionCreated]

        // Send message again and check response.
        repo ! SessionRepo.GetOrCreateSession("phred", Some(response.deviceId), None, probe.ref)

        inside(probe.expectMessageType[SessionRepo.Response]) {
          case SessionRepo.SessionCreated(token, deviceId) =>
            token shouldEqual response.token
            deviceId shouldEqual response.deviceId
        }
      }
    }
  }

  "ValidateToken" when {
    "JWT is valid" when {
      "Session does not exist" should {
        "respond with Invalid" in {
          val repo = testKit.spawn(SessionRepo())
          val probe = testKit.createTestProbe[SessionRepo.Response]()
          val token = Token.generateAndSign(UUID.randomUUID.toString)

          repo ! SessionRepo.ValidateToken(token, probe.ref)

          inside(probe.expectMessageType[SessionRepo.Response]) {
            case SessionRepo.Invalid(error) =>
              error.getMessage should include ("Session not found")
          }
        }
      }
    }
  }
}
