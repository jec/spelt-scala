package net.jcain.spelt.store

import net.jcain.spelt.models.User
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.FishingOutcomes.complete
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class UserStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "CreateUser" when {
    "user does not exist" should {
      "create the User and return its name" in {
        val repo = testKit.spawn(UserStore())
        val probe = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserStore.CreateUserResponse(Right("phred")))
      }
    }

    "user exists" should {
      "return an error" in {
        // Create a user.
        val repo = testKit.spawn(UserStore())
        val probe0 = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe0.ref)
        probe0.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Try it again an expect an error.
        val probe1 = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe1.ref)
        inside(probe1.expectMessageType[UserStore.Response]) {
          case UserStore.CreateUserResponse(Left(error)) =>
            error.getMessage should equal ("User \"phred\" already exists")
        }
      }
    }
  }

  "GetUser" when {
    "user does not exist" should {
      "return None" in {
        val repo = testKit.spawn(UserStore())
        val probe = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.GetUser("phred", probe.ref)
        probe.expectMessage(UserStore.GetUserResponse(Right(None)))
      }
    }

    "user exists" should {
      "return Some(User)" in {
        val repo = testKit.spawn(UserStore())

        // Create the user.
        val createProbe = testKit.createTestProbe[UserStore.Response]()
        repo ! UserStore.CreateUser("phred", "secret", "phred@example.com", createProbe.ref)
        createProbe.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Check the response for GetUser.
        val getProbe = testKit.createTestProbe[UserStore.Response]()
        repo ! UserStore.GetUser("phred", getProbe.ref)
        getProbe.fishForMessagePF(3.seconds) {
          case UserStore.GetUserResponse(Right(Some(User("phred", _, "phred@example.com")))) =>
            complete
        }
      }
    }

    "database query raises an exception" should {
      "return None" in {
        // TODO: Implement test.
      }
    }
  }

  "UserInquiry" when {
    "user does not exist" should {
      "return false" in {
        val repo = testKit.spawn(UserStore())
        val probe = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.UserInquiry("phred", probe.ref)
        probe.expectMessage(UserStore.UserInquiryResponse(Right(false)))
      }
    }

    "user exists" should {
      "return true" in {
        // Create a user.
        val repo = testKit.spawn(UserStore())
        val probe0 = testKit.createTestProbe[UserStore.Response]()

        repo ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe0.ref)
        probe0.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Check the response for UserInquiry.
        val userExistsProbe = testKit.createTestProbe[UserStore.Response]()
        repo ! UserStore.UserInquiry("phred", userExistsProbe.ref)
        userExistsProbe.expectMessage(UserStore.UserInquiryResponse(Right(true)))
      }
    }
  }
}
