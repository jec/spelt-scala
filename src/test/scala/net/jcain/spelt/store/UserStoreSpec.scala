package net.jcain.spelt.store

import net.jcain.spelt.models.User
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.FishingOutcomes.complete
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class UserStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait TargetActor {
    val actor: ActorRef[UserStore.Request] = testKit.spawn(UserStore())
    val probe: TestProbe[UserStore.Response] = testKit.createTestProbe[UserStore.Response]()
  }

  "CreateUser" when {
    "user does not exist" should {
      "create the User and return its name" in new TargetActor {
        actor ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserStore.CreateUserResponse(Right("phred")))
      }
    }

    "user exists" should {
      "return an error" in new TargetActor {
        // Create User.
        actor ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Try it again an expect an error.
        private val probe1 = testKit.createTestProbe[UserStore.Response]()

        actor ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe1.ref)
        inside(probe1.expectMessageType[UserStore.Response]) {
          case UserStore.CreateUserResponse(Left(error)) =>
            error.getMessage should equal ("User \"phred\" already exists")
        }
      }
    }
  }

  "GetUser" when {
    "user does not exist" should {
      "return None" in new TargetActor {
        actor ! UserStore.GetUser("phred", probe.ref)
        probe.expectMessage(UserStore.GetUserResponse(Right(None)))
      }
    }

    "user exists" should {
      "return Some(User)" in new TargetActor {
        // Create the user.
        private val createProbe = testKit.createTestProbe[UserStore.Response]()
        actor ! UserStore.CreateUser("phred", "secret", "phred@example.com", createProbe.ref)
        createProbe.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Check the response for GetUser.
        private val getProbe = testKit.createTestProbe[UserStore.Response]()
        actor ! UserStore.GetUser("phred", getProbe.ref)
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
      "return false" in new TargetActor {
        actor ! UserStore.UserInquiry("phred", probe.ref)
        probe.expectMessage(UserStore.UserInquiryResponse(Right(false)))
      }
    }

    "user exists" should {
      "return true" in new TargetActor {
        // Create User.
        actor ! UserStore.CreateUser("phred", "secret", "phred@example.com", probe.ref)
        probe.expectMessage(UserStore.CreateUserResponse(Right("phred")))

        // Check the response for UserInquiry.
        private val userExistsProbe = testKit.createTestProbe[UserStore.Response]()
        actor ! UserStore.UserInquiry("phred", userExistsProbe.ref)
        userExistsProbe.expectMessage(UserStore.UserInquiryResponse(Right(true)))
      }
    }
  }
}
