package net.jcain.spelt

import com.google.inject.{AbstractModule, Provides}
import neotypes.{AsyncDriver, GraphDatabase}
import net.jcain.spelt.models.Config
import net.jcain.spelt.service.{Auth, Events, Main, Rooms}
import net.jcain.spelt.store.{EventStore, RoomStore, SessionStore, UserStore}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.neo4j.driver.AuthTokens
import play.api.libs.concurrent.PekkoGuiceSupport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object Module:
  private var _driver: Option[AsyncDriver[Future]] = None
  private var _system: Option[ActorSystem[Main.Request]] = None

/**
 * Instantiates actors and binds them for injection
 */
class Module extends AbstractModule with PekkoGuiceSupport:
  override def configure(): Unit =
    // TODO: This should start a single supervisor actor, which is responsible for starting and
    //   restarting all of the others.
    bind(classOf[EventStore.Request]).asEagerSingleton()
    bind(classOf[RoomStore.Request]).asEagerSingleton()
    bind(classOf[SessionStore.Request]).asEagerSingleton()
    bind(classOf[UserStore.Request]).asEagerSingleton()
    bindTypedActor(Auth, "AuthActor")
    bindTypedActor(Events, "EventsActor")
    bindTypedActor(Rooms, "RoomActor")

  @Provides
  def system: ActorSystem[Main.Request] =
    Module._system match {
      case Some(system) =>
        system

      case None =>
        val system = ActorSystem[Main.Request](Behaviors.setup(context => Main(context)), "Main")
        Module._system = Some(system)
        system
    }

  @Inject
  @Provides
  def driver(implicit xc: ExecutionContext): AsyncDriver[Future] =
    Module._driver match {
      case Some(driver) =>
        driver

      case None =>
        val url = Config.database.getString("url")
        val username = Config.database.getString("username")
        val password = Config.database.getString("password")

        val driver = GraphDatabase.asyncDriver[Future](url, AuthTokens.basic(username, password))
        Module._driver = Some(driver)
        driver
    }
