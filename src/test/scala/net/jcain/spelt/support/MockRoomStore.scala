package net.jcain.spelt.support

import net.jcain.spelt.store.RoomStore
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MockRoomStore:
  def apply(): Behavior[RoomStore.Request] = Behaviors.receiveMessage:
    case RoomStore.CreateRoom(roomRequest, replyTo) =>
      Behaviors.same