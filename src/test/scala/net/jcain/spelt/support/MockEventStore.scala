package net.jcain.spelt.support

import net.jcain.spelt.store.EventStore
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MockEventStore:
  def apply(): Behavior[EventStore.Request] = Behaviors.receiveMessage:
    case EventStore.CreateEventsForNewRoom(roomId, request, user, replyTo) =>
      replyTo ! EventStore.CreateEventsForNewRoomResponse(Right(()))
      Behaviors.same
