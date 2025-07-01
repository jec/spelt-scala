package net.jcain.spelt.service

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import javax.inject.Inject

object Main:
  sealed trait Request

  sealed trait Response

class Main (context: ActorContext[Main.Request]) extends AbstractBehavior[Main.Request](context):
  import Main.*

  def onMessage(message: Request): Behavior[Main.Request] =
    Behaviors.same
