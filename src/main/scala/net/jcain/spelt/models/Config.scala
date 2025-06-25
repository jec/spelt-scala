package net.jcain.spelt.models

import com.typesafe.config
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsValue, Json}

object Config {
  private val base: config.Config = ConfigFactory.load()
  val database: config.Config = base.getConfig("database")
  val versions: Seq[String] = Seq("1.14")
  val jwtIssuer: String = base.getString("jwt.issuer")

  val homeserver: String = Config.base.getString("server.homeserver_name")
  val identityUrl: String = Config.base.getString("server.identity_server_url")

  val wellKnown: JsValue = Json.obj(
    "m.homeserver" -> Json.obj("base_url" -> s"https://$homeserver"),
    "m.identity_server" -> Json.obj("base_url" -> identityUrl)
  )
}
