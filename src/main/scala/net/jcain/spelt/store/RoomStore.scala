package net.jcain.spelt.store

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object RoomStore:
  sealed trait Request
  final case class CreateRoom(name: String) extends Request

  sealed trait Response

  def apply(): Behavior[Request] = Behaviors.receiveMessage:
    case CreateRoom(name) =>
      Behaviors.same
