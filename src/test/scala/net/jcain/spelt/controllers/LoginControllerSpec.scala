package net.jcain.spelt.controllers

import org.scalatra.test.scalatest.ScalatraWordSpec

class LoginControllerSpec extends ScalatraWordSpec {
  addServlet(classOf[LoginController], "/_matrix/client/v3/*")

  "GET /_matrix/client/v3/login" should {
    "return the available login types" in {
      get("_matrix/client/v3/login") {
        status should equal (200)
        body should equal ("{\"flows\":[{\"type\":\"m.login.password\"}]}")
      }
    }
  }
}
