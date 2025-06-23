package net.jcain.spelt

import com.google.inject.AbstractModule
import net.jcain.spelt.service.Auth
import net.jcain.spelt.store.{SessionStore, UserStore}
import play.api.Logger
import play.api.libs.concurrent.PekkoGuiceSupport

class Module extends AbstractModule with PekkoGuiceSupport {
  override def configure(): Unit =
//    val logger: Logger = Logger(this.getClass)

//    logger.debug { "In configure()" }

    throw new NotImplementedError("oops")
    bindTypedActor(UserStore(), "UserStoreActor")
    bindTypedActor(SessionStore(), "SessionStoreActor")
    bindTypedActor(Auth, "AuthActor")
}
