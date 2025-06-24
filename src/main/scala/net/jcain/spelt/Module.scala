package net.jcain.spelt

import com.google.inject.AbstractModule
import net.jcain.spelt.service.Auth
import net.jcain.spelt.store.{SessionStore, UserStore}
import play.api.libs.concurrent.PekkoGuiceSupport

/**
 * Instantiates actors and binds them for injection
 */
class Module extends AbstractModule with PekkoGuiceSupport {
  override def configure(): Unit =
    bindTypedActor(UserStore(), "UserStoreActor")
    bindTypedActor(SessionStore(), "SessionStoreActor")
    bindTypedActor(Auth, "AuthActor")
}
