package net.jcain.spelt

import com.google.inject.AbstractModule
import net.jcain.spelt.service.{Auth, Events, Rooms}
import net.jcain.spelt.store.{EventStore, RoomStore, SessionStore, UserStore}
import play.api.libs.concurrent.PekkoGuiceSupport

/**
 * Instantiates actors and binds them for injection
 */
class Module extends AbstractModule with PekkoGuiceSupport {
  override def configure(): Unit =
    bindTypedActor(EventStore(), "EventStoreActor")
    bindTypedActor(RoomStore(), "RoomStoreActor")
    bindTypedActor(SessionStore(), "SessionStoreActor")
    bindTypedActor(UserStore(), "UserStoreActor")
    bindTypedActor(Auth, "AuthActor")
    bindTypedActor(Events, "EventsActor")
    bindTypedActor(Rooms, "RoomActor")
}
