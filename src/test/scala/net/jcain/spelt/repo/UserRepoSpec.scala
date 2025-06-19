package net.jcain.spelt.repo

import net.jcain.spelt.models.User
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.FishingOutcomes.complete
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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
        inside(probe1.expectMessageType[UserRepo.Response]) {
          case UserRepo.CreateUserResponse(Left(error)) =>
            error.getMessage should equal ("User \"phred\" already exists")
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
        getProbe.fishForMessagePF(3.seconds) {
          case UserRepo.GetUserResponse(Some(User("phred", _, "phred@example.com"))) =>
            complete
        }
      }
    }

    "when database query raises an exception" should {
      "return None" in {
        // TODO: Implement test.
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
