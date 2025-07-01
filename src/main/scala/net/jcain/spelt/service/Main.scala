package net.jcain.spelt.service

import neotypes.AsyncDriver
import net.jcain.spelt.store.{SessionStore, UserStore}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object Main:
  private var _authRef: Option[ActorRef[Auth.Request]] = None
  private var _executionContext: Option[ExecutionContext] = None
  private var _sessionStoreRef: Option[ActorRef[SessionStore.Request]] = None
  private var _userStoreRef: Option[ActorRef[UserStore.Request]] = None

  sealed trait Request

  sealed trait Response

  def authRef: Option[ActorRef[Auth.Request]] = _authRef
  def executionContext: Option[ExecutionContext] = _executionContext
  def sessionStoreRef: Option[ActorRef[SessionStore.Request]] = _sessionStoreRef
  def userStoreRef: Option[ActorRef[UserStore.Request]] = _userStoreRef

  @Inject()
  def apply(context: ActorContext[Main.Request])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Behavior[Request] =
    _executionContext = Some(xc)

    _sessionStoreRef = Some(context.spawn(SessionStore(), "SessionStore"))
    context.watch(_sessionStoreRef.get)

    _userStoreRef = Some(context.spawn(UserStore(), "UserStore"))
    context.watch(_userStoreRef.get)

    _authRef = Some(context.spawn(Auth(_userStoreRef.get, _sessionStoreRef.get), "Auth"))
    context.watch(_authRef.get)

    Behaviors.receiveMessage {
      case _ =>
        Behaviors.same
    }
