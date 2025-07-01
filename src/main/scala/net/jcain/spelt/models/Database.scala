package net.jcain.spelt.models

import neotypes.{AsyncDriver, GraphDatabase}
import org.neo4j.driver.AuthTokens

import scala.concurrent.Future

/**
 * Instantiates a single database driver to be used throughout the application
 */
object Database {
/*
  var driver: AsyncDriver[Future] = {
    val url = Config.database.getString("url")
    val username = Config.database.getString("username")
    val password = Config.database.getString("password")

    GraphDatabase.asyncDriver[Future](url, AuthTokens.basic(username, password))
  }
*/
}
