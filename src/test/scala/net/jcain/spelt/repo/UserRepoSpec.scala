package net.jcain.spelt.repo

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import net.jcain.spelt.models.User
import net.jcain.spelt.support.DatabaseRollback
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserRepoSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  "CreateUser" when {
    "user does not exist" should {
      "create the User and return its identifier" in {
        val repo = testKit.spawn(UserRepo())
        val probe = testKit.createTestProbe[UserRepo.Response]()

        repo ! UserRepo.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserRepo.CreateUserResponse(Right("phred")))
        println
      }
    }

    "user exists" should {
      "return an error" in {
//        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com")
//
//        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com") should matchPattern {
//          case Left(error) =>
//        }
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
        getProbe.expectMessage(UserRepo.GetUserResponse(Some(User("phred", "foo", "phred@example.com"))))
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
        val repo = testKit.spawn(UserRepo())

        //        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com")

        val userExistsProbe = testKit.createTestProbe[UserRepo.Response]()
        repo ! UserRepo.UserInquiry("phred", userExistsProbe.ref)
        userExistsProbe.expectMessage(UserRepo.UserInquiryResponse(true))
      }
    }
  }
}
