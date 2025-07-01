package net.jcain.spelt.support

import neotypes.AsyncDriver
import neotypes.syntax.all.*
import net.jcain.spelt.models.Database
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.concurrent.*
import scala.concurrent.duration.*

trait DatabaseRollback(driver: AsyncDriver[Future]) extends BeforeAndAfterEach { this: Suite =>
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
