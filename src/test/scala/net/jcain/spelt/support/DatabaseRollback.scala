package net.jcain.spelt.support

import neotypes.syntax.all.c
import neotypes.{AsyncDriver, GraphDatabase}
import net.jcain.spelt.models.Config
import org.neo4j.driver.AuthTokens
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

trait DatabaseRollback extends BeforeAndAfterEach { this: Suite =>
  // TODO: Would be nice to use Play's EC here, but injection doesn't work because changing the
  //   test classes' signatures causes the test runner to skip them.
  import scala.concurrent.ExecutionContext.Implicits.global

  private val url = Config.database.getString("url")
  private val username = Config.database.getString("username")
  private val password = Config.database.getString("password")

  implicit val driver: AsyncDriver[Future] = GraphDatabase.asyncDriver[Future](url, AuthTokens.basic(username, password))

  override def beforeEach(): Unit = {
    wipeData()
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    wipeData()
  }

  private def wipeData(): Unit = {
    Await.result(
      c"MATCH (x) DETACH DELETE x".execute.void(driver),
      5.minutes
    )
  }
}
