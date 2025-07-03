package net.jcain.spelt

import com.google.inject.{AbstractModule, Provides}
import neotypes.{AsyncDriver, GraphDatabase}
import net.jcain.spelt.models.Config
import net.jcain.spelt.service.{Auth, Main}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.neo4j.driver.AuthTokens
import play.api.libs.concurrent.PekkoGuiceSupport

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object Module:
  private var _driver: Option[AsyncDriver[Future]] = None
  private var _system: Option[ActorSystem[Main.Request]] = None

  def driver: AsyncDriver[Future] = _driver.get

/**
 * Instantiates actors and binds them for injection
 */
class Module extends AbstractModule with PekkoGuiceSupport:
  import Module.*

  /**
   * Called by Guice to bind all of our dependency-injected objects
   */
    override def configure(): Unit =
      import scala.concurrent.ExecutionContext.Implicits.global
      system
      driver

  /**
   * Lazily instantiates a typed `ActorSystem` that uses `Main` as the root actor
   *
   * This is marked as a provider to Guice for the returned type.
   *
   * @return the typed actor system used for all of our own actors
   */
  @Inject
  @Provides
  def system(implicit xc: ExecutionContext): ActorSystem[Main.Request] = {
    _system match {
      case Some(system) =>
        system

      case None =>
        val system = ActorSystem[Main.Request](
          Behaviors.setup[Main.Request] { (context: ActorContext[Main.Request]) =>
            context.spawn(Main(context), "Main")
            Behaviors.same
          },
          "Main"
        )
        _system = Some(system)
        system
    }
  }

  /**
   * Lazily instantiates a neotypes `AsyncDriver`
   *
   * This is marked as a provider to Guice for the returned type.
   *
   * @return the only Neo4j driver object used throughout the application
   */
  @Inject
  @Provides
  implicit def driver(implicit xc: ExecutionContext): AsyncDriver[Future] =
    _driver match {
      case Some(driver) =>
        driver

      case None =>
        val url = Config.database.getString("url")
        val username = Config.database.getString("username")
        val password = Config.database.getString("password")

        val driver = GraphDatabase.asyncDriver[Future](url, AuthTokens.basic(username, password))
        _driver = Some(driver)
        driver
    }

  @Provides
  def authRef: ActorRef[Auth.Request] = Main.authRef.get
