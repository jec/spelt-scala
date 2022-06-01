package net.jcain.spelt.controllers

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatra.test.scalatest._

class SpeltControllerSpec extends TestKit(ActorSystem("MySpec")) with ScalatraWordSpec {
  addServlet(new SpeltController(system), "/*")

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "GET /" should {
    "return the message" in {
      get("/Hello+world") {
        status should equal (200)
        body should equal ("{\"message\":\"Hello world\"}")
      }
    }
  }
}
