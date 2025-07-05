package net.jcain.spelt.service

import neotypes.AsyncDriver
import net.jcain.spelt.store.{EventStore, RoomStore, SessionStore, UserStore}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, Terminated}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object Main:
  private var _authRef: Option[ActorRef[Auth.Request]] = None
  private var _eventsRef: Option[ActorRef[Events.Request]] = None
  private var _eventStoreRef: Option[ActorRef[EventStore.Request]] = None
  private var _executionContext: Option[ExecutionContext] = None
  private var _roomStoreRef: Option[ActorRef[RoomStore.Request]] = None
  private var _roomsRef: Option[ActorRef[Rooms.Request]] = None
  private var _sessionStoreRef: Option[ActorRef[SessionStore.Request]] = None
  private var _userStoreRef: Option[ActorRef[UserStore.Request]] = None

  sealed trait Request

  sealed trait Response

  def authRef: Option[ActorRef[Auth.Request]] = _authRef
  def executionContext: Option[ExecutionContext] = _executionContext
  def roomsRef: Option[ActorRef[Rooms.Request]] = _roomsRef
  def sessionStoreRef: Option[ActorRef[SessionStore.Request]] = _sessionStoreRef

  @Inject()
  def apply()(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Behavior[Request] =
    _executionContext = Some(xc)

    Behaviors.setup { context =>
      context.log.info("Spawning EventStore")
      _eventStoreRef = Some(context.spawn(EventStore(), "EventStore"))
      context.watch(_eventStoreRef.get)

      context.log.info("Spawning RoomStore")
      _roomStoreRef = Some(context.spawn(RoomStore(), "RoomStore"))
      context.watch(_roomStoreRef.get)

      context.log.info("Spawning SessionStore")
      _sessionStoreRef = Some(context.spawn(SessionStore(), "SessionStore"))
      context.watch(_sessionStoreRef.get)

      context.log.info("Spawning UserStore")
      _userStoreRef = Some(context.spawn(UserStore(), "UserStore"))
      context.watch(_userStoreRef.get)

      context.log.info("Spawning Auth")
      _authRef = Some(context.spawn(Auth(_userStoreRef.get, _sessionStoreRef.get), "Auth"))
      context.watch(_authRef.get)

      context.log.info("Spawning Events")
      _eventsRef = Some(context.spawn(Events(_eventStoreRef.get), "Events"))
      context.watch(_eventsRef.get)

      context.log.info("Spawning Rooms")
      _roomsRef = Some(context.spawn(Rooms(_eventStoreRef.get, _roomStoreRef.get), "Rooms"))
      context.watch(_roomsRef.get)

      Behaviors
        .receive[Request] { (_, message) =>
          message match {
            case _ =>
              Behaviors.same
          }
        }
        .receiveSignal {
          case (_, Terminated(ref: ActorRef[SessionStore.Request])) =>
            Behaviors.same
        }
    }
