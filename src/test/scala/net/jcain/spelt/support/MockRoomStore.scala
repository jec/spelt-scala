package net.jcain.spelt.support

import net.jcain.spelt.models.Room
import net.jcain.spelt.store.RoomStore
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MockRoomStore:
  def apply(room: Room): Behavior[RoomStore.Request] = Behaviors.receiveMessage:
    case RoomStore.CreateRoom(roomRequest, replyTo) =>
      replyTo ! RoomStore.CreateRoomResponse(Right(room))
      Behaviors.same
