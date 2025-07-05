package net.jcain.spelt.store

import neotypes.AsyncDriver
import neotypes.generic.implicits.*
import neotypes.syntax.all.c
import net.jcain.spelt.models.User
import net.jcain.spelt.models.events.MRoomCreate
import net.jcain.spelt.models.requests.CreateRoomRequest
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import play.api.Logging
import wvlet.airframe.ulid.ULID

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object EventStore extends Logging:
  sealed trait Request
  final case class CreateEventsForNewRoom(roomId: String, request: CreateRoomRequest, user: User, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateEventsForNewRoomResponse(unitOrError: Either[String, Unit]) extends Response

  @Inject()
  def apply()(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Behavior[Request] =
    Behaviors.receiveMessage:
      case CreateEventsForNewRoom(roomId, request, user, replyTo) =>
        createEventsForNewRoom(roomId, request, user, replyTo)
        Behaviors.same

  private def createEventsForNewRoom(roomId: String, request: CreateRoomRequest, user: User, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit = {
    // Event m.room.create
    // Event m.room.member
    // Event m.room.power_levels
    // Event m.room.canonical_alias if `room_alias_name`
    // Events in `preset`
    // Events in `initial_state`
    // Event m.room_name if `name`
    // Event m.room.topic if `topic`
    // Events from `invite` and `invite_3pid`

    // Initiate the Futures in the `for` so that they're run sequentially, which is required by the
    // Matrix specification.
    (for
      e0 <- createRoomCreateEvent(roomId, request, user)
//        e1 <- createRoomMemberEvent(roomId, request) if e0.counters.nodesCreated > 1
    yield e0)
      .onComplete:
        case Failure(error) =>
          logger.error(error.toString)
          logger.error(error.getStackTrace.mkString("[", ";", "]"))
          logger.error(error.getCause.toString)
          logger.error(error.getCause.getStackTrace.mkString("[\n", ";\n", "]"))
          replyTo ! CreateEventsForNewRoomResponse(Left(error.getMessage))
        case Success(rs0) =>
          replyTo ! CreateEventsForNewRoomResponse(Right(()))
  }

  private def createRoomCreateEvent(roomId: String, request: CreateRoomRequest, user: User)(implicit driver: AsyncDriver[Future], xc: ExecutionContext) = {
    val event = MRoomCreate(ULID.newULIDString, ULID.newULIDString, 0)

    c"""MATCH (r:Room) WHERE r.identifier = $roomId
        CREATE (e:#${event.label} {$event})-[:SENT_TO]->(r)
      """
      .execute
      .resultSummary(driver)
  }

  private def createRoomMemberEvent(roomId: String, request: CreateRoomRequest)(implicit driver: AsyncDriver[Future], xc: ExecutionContext) =
    c"CREATE"
      .execute
      .resultSummary(driver)
