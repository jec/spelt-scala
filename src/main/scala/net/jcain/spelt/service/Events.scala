package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.models.User
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.store.EventStore
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import play.api.libs.concurrent.ActorModule

/**
  * An Actor that implements the logic around `Event`s
  *
  * This actor interacts with the `EventStore` as necessary to request CRUD actions on persistent
  * data relevant to Events. Controllers and other actors should use this actor instead of
  * EventStore when needing such Event-related CRUD actions.
  *
  * Messages it receives:
  *
  *   - `Foo` — To do
  *
  *     Responses:
  *
  *       - `FooResponse` — wraps a `Right(Unit)` if successful; else a
  *         `Left(errorMessage: String)`
  */
object Events extends ActorModule:
  type Message = Request

  sealed trait Request

  sealed trait Response

  @Provides
  def apply(eventStore: ActorRef[EventStore.Request]): Behavior[Request] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case _ =>
        Behaviors.same
    }
  }
