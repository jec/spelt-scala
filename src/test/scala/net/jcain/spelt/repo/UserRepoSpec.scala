package net.jcain.spelt.repo

import akka.actor.testkit.typed.scaladsl.FishingOutcomes.complete
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.auth0.jwt.interfaces.DecodedJWT
import net.jcain.spelt.models.User
import net.jcain.spelt.service.Token
import net.jcain.spelt.support.DatabaseRollback
import org.scalatest.Inside.inside
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.lang.Thread.sleep
import scala.concurrent.duration.DurationInt

class UserRepoSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "CreateUser" when {
    "user does not exist" should {
      "create the User and return its identifier" in {
        val repo = testKit.spawn(UserRepo())
        val probe = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserRepo.CreateUserResponse(Right("phred")))
      }
    }

    "user exists" should {
      "return an error" in {
        // Create a user.
        val repo = testKit.spawn(UserRepo())
        val probe0 = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", probe0.ref)
        probe0.expectMessage(UserRepo.CreateUserResponse(Right("phred")))

        // Try it again an expect an error.
        val probe1 = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", probe1.ref)
        probe1.expectMessageType[UserRepo.Response] must matchPattern {
          case UserRepo.CreateUserResponse(Left(_)) =>
        }
      }
    }
  }

  "GetUser" when {
    "user does not exist" should {
      "return None" in {
        val repo = testKit.spawn(UserRepo())
        val probe = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.GetUser("phred", probe.ref)
        probe.expectMessage(UserRepo.GetUserResponse(None))
      }
    }

    "when user exists" should {
      "return Some(User)" in {
        val repo = testKit.spawn(UserRepo())

        // Create the user.
        val createProbe = testKit.createTestProbe[UserRepo.Response]()
        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", createProbe.ref)
        createProbe.expectMessage(UserRepo.CreateUserResponse(Right("phred")))

        // Check the response for GetUser.
        val getProbe = testKit.createTestProbe[UserRepo.Response]()
        repo ! UserRepo.GetUser("phred", getProbe.ref)
        getProbe.fishForMessagePF(5.seconds) {
          case UserRepo.GetUserResponse(Some(User("phred", _, "phred@example.com"))) =>
            complete
        }
      }
    }
  }

  "UserInquiry" when {
    "user does not exist" should {
      "return false" in {
        val repo = testKit.spawn(UserRepo())
        val probe = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.UserInquiry("phred", probe.ref)
        probe.expectMessage(UserRepo.UserInquiryResponse(false))
      }
    }

    "user exists" should {
      "return true" in {
        // Create a user.
        val repo = testKit.spawn(UserRepo())
        val probe0 = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", probe0.ref)
        probe0.expectMessage(UserRepo.CreateUserResponse(Right("phred")))

        // Check the response for UserInquiry.
        val userExistsProbe = testKit.createTestProbe[UserRepo.Response]()
        repo ! UserRepo.UserInquiry("phred", userExistsProbe.ref)
        userExistsProbe.expectMessage(UserRepo.UserInquiryResponse(true))
      }
    }
  }
}
