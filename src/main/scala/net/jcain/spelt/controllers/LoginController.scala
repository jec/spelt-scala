package net.jcain.spelt.controllers

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport

class LoginController extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/login") {
    Ok(Map("flows" -> Seq(Map("type" -> "m.login.password"))))
  }

  post("/login") {
    Ok(Map("flows" -> Seq(Map("type" -> "m.login.password"))))
  }
}
