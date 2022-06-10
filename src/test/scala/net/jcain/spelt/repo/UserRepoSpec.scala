package net.jcain.spelt.repo

import net.jcain.spelt.models.User
import net.jcain.spelt.support.DatabaseRollback
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserRepoSpec extends AnyWordSpecLike with Matchers with DatabaseRollback {
  "userExists()" when {
    "user does not exist" should {
      "return false" in {
        UserRepo.userExists("phred") should equal (false)
      }
    }

    "user exists" should {
      "return true" in {
        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com")

        UserRepo.userExists("phred") should equal (true)
      }
    }
  }

  "createUser()" when {
    "user does not exist" should {
      "create the User and return its identifier" in {
        UserRepo.createUser("phred", "secret", "Phred", "phred@example.com") should matchPattern {
          case Right("phred") =>
        }
        UserRepo.userExists("phred") should equal (true)
      }
    }

    "user exists" should {
      "return an error" in {
        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com")

        UserRepo.createUser("phred", "bar", "Phred", "phred@example.com") should matchPattern {
          case Left(error) =>
        }
      }
    }
  }

  "getUser()" when {
    "user does not exist" should {
      "return None" in {
        UserRepo.getUser("phred") should equal (None)
      }
    }

    "when user exists" should {
      "return Some(User)" in {
        UserRepo.createUser("phred", "secret", "Phred", "phred@example.com")

        UserRepo.getUser("phred") should matchPattern {
          case Some(User("phred", _, "Phred", "phred@example.com")) =>
        }
      }
    }
  }
}
