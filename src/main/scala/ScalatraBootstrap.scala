import akka.actor.ActorSystem
import net.jcain.spelt.controllers._
import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()

  override def init(context: ServletContext): Unit = {
    context.mount(new ConfigController, "/*")
    context.mount(new LoginController, "/_matrix/client/v3/*")
  }
}
