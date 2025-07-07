package net.jcain.spelt.store

import neotypes.AsyncDriver
import neotypes.generic.implicits.*
import neotypes.syntax.all.c
import net.jcain.spelt.models.User
import net.jcain.spelt.models.events.{MRoomCreate, MRoomMember, MRoomPowerLevels}
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
      (s0, id0) <- createRoomCreateEvent(roomId, request, user)
      (s1, id1) <- createRoomMemberEvent(roomId, request, id0) if s0.counters.nodesCreated == 1
      (s2, id2) <- createRoomPowerLevelsEvent(roomId, request, id1) if s1.counters.nodesCreated == 1
    yield (s2,id2))
      .onComplete:
        case Failure(error) =>
          logger.error(error.toString)
          logger.error(error.getStackTrace.mkString("[", ";", "]"))
          Option(error.getCause) match {
            case None =>
            case Some(cause) =>
              logger.error(cause.toString)
              logger.error(cause.getStackTrace.mkString("[\n", ";\n", "]"))
          }
          replyTo ! CreateEventsForNewRoomResponse(Left(error.getMessage))
        case Success(_) =>
          replyTo ! CreateEventsForNewRoomResponse(Right(()))
  }

  private def createRoomCreateEvent(roomId: String, request: CreateRoomRequest, user: User)(implicit driver: AsyncDriver[Future], xc: ExecutionContext) =
    val event = MRoomCreate()

    c"""
      MATCH (r:Room) WHERE r.identifier = $roomId
      CREATE (:#${event.label} {$event})-[:SENT_TO]->(r)
    """
      .execute
      .resultSummary(driver)
      .map(summary => { println(summary.counters); (summary, event.identifier) })

  private def createRoomMemberEvent(roomId: String, request: CreateRoomRequest, parentId: String)(implicit driver: AsyncDriver[Future], xc: ExecutionContext) =
    val event = MRoomMember()

    c"""
      MATCH (r:Room) WHERE r.identifier = $roomId
      MATCH (e0:Event) WHERE e0.identifier = $parentId
      CREATE (e:#${event.label} {$event}),
        (e)-[:SENT_TO]->(r),
        (e)-[:CHILD_OF]->(e0)
      SET e.depth = e0.depth + 1
    """
      .execute
      .resultSummary(driver)
      .map(summary => { println(summary.counters); (summary, event.identifier) })

private def createRoomPowerLevelsEvent(roomId: String, request: CreateRoomRequest, parentId: String)(implicit driver: AsyncDriver[Future], xc: ExecutionContext) =
  val event = MRoomPowerLevels()

  c"""
    MATCH (r:Room) WHERE r.identifier = $roomId
    MATCH (e0:Event) WHERE e0.identifier = $parentId
    CREATE (e:#${event.label} {$event}),
      (e)-[:SENT_TO]->(r),
      (e)-[:CHILD_OF]->(e0)
    SET e.depth = e0.depth + 1
  """
    .execute
    .resultSummary(driver)
    .map(summary => { println(summary.counters); (summary, event.identifier) })
