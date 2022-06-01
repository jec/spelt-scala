package net.jcain.spelt.models

import org.neo4j.driver.async.AsyncSession
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase}

object Database {
  protected var db: Driver = {
    val url = Config.database.getString("url")
    val username = Config.database.getString("username")
    val password = Config.database.getString("password")

    GraphDatabase.driver(url, AuthTokens.basic(username, password))
  }

  def getSession: AsyncSession = { db.asyncSession() }
}
