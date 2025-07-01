package net.jcain.spelt.store

import neotypes.AsyncDriver
import neotypes.syntax.all.*
import net.jcain.spelt.models.requests.CreateRoomRequest
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object EventStore:
  sealed trait Request
  final case class CreateEventsForNewRoom(roomId: String, request: CreateRoomRequest, replyTo: ActorRef[Response]) extends Request

  sealed trait Response

class EventStore @Inject()(context: ActorContext[EventStore.Request],
                           driver: AsyncDriver[Future])(implicit xc: ExecutionContext) extends AbstractBehavior[EventStore.Request](context):
  import EventStore.*

  def onMessage(message: Request): Behavior[Request] =
    message match {
      case CreateEventsForNewRoom(roomId, request, replyTo) =>
        createEventsForNewRoom(roomId, request, replyTo)
        Behaviors.same
    }

  private def createEventsForNewRoom(roomId: String, request: CreateRoomRequest, replyTo: ActorRef[Response])(implicit xc: ExecutionContext): Unit = {
    val result =
      for
        e0 <- createRoomCreateEvent(roomId, request)
        e1 <- createRoomMemberEvent(roomId, request)
      yield
        (e0, e1)

    // Event m.room.create
    // Event m.room.member
    // Event m.room.power_levels
    // Event m.room.canonical_alias if `room_alias_name`
    // Events in `preset`
    // Events in `initial_state`
    // Event m.room_name if `name`
    // Event m.room.topic if `topic`
    // Events from `invite` and `invite_3pid`
    ()
  }

  private def createRoomCreateEvent(roomId: String, request: CreateRoomRequest) =
    c"CREATE"
      .execute
      .resultSummary(driver)

  private def createRoomMemberEvent(roomId: String, request: CreateRoomRequest) =
    c"CREATE"
      .execute
      .resultSummary(driver)
