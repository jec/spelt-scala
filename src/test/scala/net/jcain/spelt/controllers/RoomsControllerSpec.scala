package net.jcain.spelt.controllers

import net.jcain.spelt.support.DatabaseRollback
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class RoomsControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with DatabaseRollback {
  "POST /_matrix/client/v3/createRoom" when {
  }
}
