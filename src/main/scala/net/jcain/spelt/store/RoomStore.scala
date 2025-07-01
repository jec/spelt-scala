package net.jcain.spelt.store

import neotypes.AsyncDriver
import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.requests.CreateRoomRequest
import net.jcain.spelt.models.{Config, Room}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import wvlet.airframe.ulid.ULID

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object RoomStore:
  sealed trait Request
  final case class CreateRoom(roomRequest: CreateRoomRequest, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateRoomResponse(roomOrError: Either[String, Room]) extends Response

  @Inject()
  def apply()(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Behavior[Request] =
    Behaviors.receiveMessage:
      case CreateRoom(roomRequest, replyTo) =>
        createRoom(roomRequest, replyTo)
        Behaviors.same

  /**
   * Creates a Room node and, if successful, responds with the new `Room`
   *
   * Once a Room node is created, several Event nodes are created which may modify the Room, so the
   * Room object received from this method is likely to be obsolete within milliseconds.
   *
   * @param roomRequest parsed body from the API request
   * @param replyTo Actor that receives response
   */
  private def createRoom(roomRequest: CreateRoomRequest, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit = {
    val identifier = ULID.newULIDString
    val roomVersion = roomRequest.room_version.getOrElse(Config.defaultNewRoomVersion)

    c"""CREATE (r:Room {
          identifier: $identifier,
          name: ${roomRequest.name},
          alias: ${roomRequest.room_alias_name},
          topic: ${roomRequest.topic},
          avatar: null,
          roomVersion: $roomVersion
        })
        RETURN r"""
      .query(ResultMapper.productDerive[Room])
      .withResultSummary
      .single(driver)
      .onComplete:
        case Failure(error) =>
          replyTo ! CreateRoomResponse(Left(error.getMessage))
        case Success((room, summary)) if summary.counters.nodesCreated == 1 =>
          replyTo ! CreateRoomResponse(Right(room))
        case Success((_, summary)) =>
          replyTo ! CreateRoomResponse(Left(s"Unknown error: nodesCreated was ${summary.counters.nodesCreated}"))
  }
