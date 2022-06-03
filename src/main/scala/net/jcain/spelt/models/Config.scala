package net.jcain.spelt.models

import com.typesafe.config
import com.typesafe.config.ConfigFactory

object Config {
  val base: config.Config = ConfigFactory.load()
  val database: config.Config = base.getConfig("database")
  val versions: Seq[String] = Seq("1.2")
  val jwtIssuer: String = base.getString("jwt.issuer")

  val homeserverUrl: String = Config.base.getString("server.base_url")
  val identityUrl: String = Config.base.getString("server.identity_server")

  val wellKnown: Map[String, Map[String, String]] = Map(
    "m.homeserver" -> Map("base_url" -> homeserverUrl),
    "m.identity_server" -> Map("base_url" -> identityUrl)
  )
}
