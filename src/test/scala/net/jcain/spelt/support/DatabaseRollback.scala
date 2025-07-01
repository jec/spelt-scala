package net.jcain.spelt.support

import neotypes.syntax.all.c
import net.jcain.spelt.Module
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

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
    Await.result(
      c"MATCH (x) DETACH DELETE x".execute.void(Module.driver),
      5.minutes
    )
  }
}
