package net.jcain.spelt.controllers

import neotypes.AsyncDriver
import net.jcain.spelt.support.DatabaseRollback
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RoomsControllerSpec @Inject() (implicit driver: AsyncDriver[Future], xc: ExecutionContext) extends PlaySpec with GuiceOneAppPerTest with Injecting with DatabaseRollback(driver) {
  "POST /_matrix/client/v3/createRoom" when {
  }
}
