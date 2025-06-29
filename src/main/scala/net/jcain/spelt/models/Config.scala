package net.jcain.spelt.models

import com.typesafe.config
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsValue, Json}

/**
 * Loads the application configuration and provides convenient access to it
 *
 * This is somewhat redundant to injecting play.api.Configuration, but injecting that here would
 * require this to be injected in many places as a singleton class, and some of those receivers
 * (notably `Database`) would also have to be injected singletons (or be replaced with bindings for
 * their values).
 */
object Config {
  private val base: config.Config = ConfigFactory.load()
  val database: config.Config = base.getConfig("database")
  val versions: Seq[String] = Seq("1.14")
  val defaultNewRoomVersion: String = "11"
  val jwtIssuer: String = base.getString("jwt.issuer")

  val homeserver: String = Config.base.getString("server.homeserver_name")
  val identityUrl: String = Config.base.getString("server.identity_server_url")

  val wellKnown: JsValue = Json.obj(
    "m.homeserver" -> Json.obj("base_url" -> s"https://$homeserver"),
    "m.identity_server" -> Json.obj("base_url" -> identityUrl)
  )
}
