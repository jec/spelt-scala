package net.jcain.spelt.repo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserRepoSpec extends AnyWordSpecLike with Matchers {
  "userExists()" when {
    "user does not exist" should {
      "return false" in {
        UserRepo.userExists("foo") should equal (false)
      }
    }
  }
}
