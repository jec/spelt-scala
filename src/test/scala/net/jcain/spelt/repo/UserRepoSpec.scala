package net.jcain.spelt.repo

import net.jcain.spelt.support.DatabaseRollback
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class UserRepoSpec extends AnyWordSpecLike with Matchers with DatabaseRollback {
  "userExists()" when {
    "user does not exist" should {
      "return false" in {
        UserRepo.userExists("foo") should equal (false)
      }
    }

    "user exists" should {
      "return true" in {
        UserRepo.createUser("foo", "bar", "Foo", "foo@example.com")

        UserRepo.userExists("foo") should equal (true)
      }
    }
  }

  "createUser()" when {
    "user does not exist" should {
      "create the User and return its UUID" in {
        inside(UserRepo.createUser("foo", "bar", "Foo", "foo@example.com")) {
          case Right(uuid) =>
            UUID.fromString(uuid) shouldBe a [UUID]
            UserRepo.userExists("foo") should equal (true)
        }
      }
    }

    "user exists" should {
      "return an error" in {
        UserRepo.createUser("foo", "bar", "Foo", "foo@example.com")

        UserRepo.createUser("foo", "bar", "Foo", "foo@example.com") should matchPattern {
          case Left(error) =>
        }
      }
    }
  }
}
