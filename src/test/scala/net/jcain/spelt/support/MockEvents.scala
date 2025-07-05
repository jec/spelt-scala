package net.jcain.spelt.support

import net.jcain.spelt.service.Events
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MockEvents:
  def apply(): Behavior[Events.Request] = Behaviors.receiveMessage:
    case _ =>
      Behaviors.same
