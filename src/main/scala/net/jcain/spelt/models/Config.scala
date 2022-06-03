package net.jcain.spelt.models

import com.typesafe.config
import com.typesafe.config.ConfigFactory

object Config {
  val base: config.Config = ConfigFactory.load()
  val database: config.Config = base.getConfig("database")
  val versions: Seq[String] = Seq("1.2")
  val jwtIssuer: String = base.getString("jwt.issuer")
}
