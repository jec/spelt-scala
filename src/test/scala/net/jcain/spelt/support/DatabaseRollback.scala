package net.jcain.spelt.support

import net.jcain.spelt.models.Database
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.jdk.FutureConverters
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

trait DatabaseRollback extends BeforeAndAfterEach { this: Suite =>
  override def beforeEach(): Unit = {
    wipeData()
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    wipeData()
  }

  private def wipeData(): Unit = {
    val session = Database.getSession

    Await.result(
      FutureConverters.CompletionStageOps(
        session.executeWriteAsync(_.runAsync("MATCH (x) DETACH DELETE x"))
      ).asScala,
      5 minutes
    )
  }
}
