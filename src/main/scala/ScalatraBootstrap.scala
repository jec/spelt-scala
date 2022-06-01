import akka.actor.ActorSystem
import net.jcain.spelt.controllers.SpeltController
import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()

  override def init(context: ServletContext): Unit = {
    context.mount(new SpeltController(system), "/*")
  }
}
