package net.jcain.spelt.store

import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.{Database, Device, Session, User}
import net.jcain.spelt.service.Token
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import wvlet.airframe.ulid.ULID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * An Actor that implements the CRUD operations for Session nodes
 *
 * Messages it receives:
 * * GetOrCreateSession -- gets (if `username` and `deviceId` are found) or creates Session and
 *     Device nodes
 *   Responses:
 *   * SessionCreated -- includes a JWT and a (possibly new) deviceId
 *   * SessionFailed -- includes a Throwable
 *
 * * DeleteSession -- deletes Session by `ulid`
 *   Responses:
 *   * SessionDeleted
 *   * SessionDeletionFailed
 *
 * * DeleteAllSessions -- deletes all Sessions and Devices belonging to `username`
 *   Responses:
 *   * AllSessionsDeleted
 *   * AllSessionsDeletionFailed
 *
 * * VerifyToken -- check whether a `token` references a Session node
 *   Responses:
 *   * TokenPassed
 *   * TokenFailed -- includes a Throwable describing the failure
 *   * TokenOtherError -- another error (e.g. Database error) occurred; includes a Throwable
 *
 * Rules:
 *   - A new login request for the same user and device invalidates any previous token issued for
 *     that device.
 *   - A login request with no device ID generates a new device ID.
 */
object SessionStore {
  sealed trait Request
  final case class GetOrCreateSession(username: String,
                                      deviceId: Option[String],
                                      deviceName: Option[String],
                                      replyTo: ActorRef[Response]) extends Request
  final case class DeleteSession(ulid: String, replyTo: ActorRef[Response]) extends Request
  final case class DeleteAllSessions(username: String, replyTo: ActorRef[Response]) extends Request
  final case class VerifyToken(token: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class SessionCreated(ulid: String, token: String, deviceId: String) extends Response
  object UserNotFound extends Response
  final case class SessionFailed(error: Throwable) extends Response
  object SessionDeleted extends Response
  final case class SessionDeletionFailed(message: String) extends Response
  object AllSessionsDeleted extends Response
  final case class AllSessionsDeletionFailed(message: String) extends Response
  final case class TokenPassed(user: User, session: Session, device: Device) extends Response
  final case class TokenFailed(error: Throwable) extends Response
  final case class TokenOtherError(error: Throwable) extends Response

  /**
   * Dispatches received messages
   *
   * @return subsequent Behaviors
   */
  def apply(): Behavior[Request] = Behaviors.receiveMessage {
    case GetOrCreateSession(name, deviceIdOption, deviceNameOption, replyTo) =>
      deviceIdOption match {
        // If deviceId isn't specified, create a new Session.
        case None =>
          createSession(name, deviceNameOption, replyTo)

        // Else look up Session.
        case Some(deviceId) =>
          readByDevice(name, deviceId, deviceNameOption, replyTo)
      }

      Behaviors.same

    case DeleteSession(ulid, replyTo) =>
      deleteSession(ulid, replyTo)
      Behaviors.same

    case DeleteAllSessions(username, replyTo) =>
      deleteAllSessions(username, replyTo)
      Behaviors.same

    case VerifyToken(token, replyTo) =>
      verifyToken(token, replyTo)
      Behaviors.same
  }

  /**
   * Looks up Session by `username` and `deviceId`; if found then updates that Session with a new
   * token; else creates a new Session and Device
   *
   * @param username username
   * @param deviceId a pre-existing device ID (optional)
   * @param deviceName a device name to use (optional)
   * @param replyTo requesting Actor
   */
  private def readByDevice(
    username: String,
    deviceId: String,
    deviceName: Option[String],
    replyTo: ActorRef[Response]
  ): Unit =
    c"""
      MATCH (u:User)-[:AUTHENTICATED_AS]->(s:Session)-[:CONNECTED_FROM]->(d:Device)
      WHERE u.name = $username
      AND d.identifier = $deviceId
      RETURN s.ulid
    """
      .query(ResultMapper.string)
      .list(Database.driver)
      .onComplete:
        case Success(ulid :: _) =>
          updateSession(ulid, replyTo)
        case Success(Nil) =>
          createSession(username, deviceName, replyTo)
        case Failure(error) =>
          replyTo ! SessionFailed(error)

  /**
   * Creates a new Session with a new token and device ID
   *
   * On successful creation, it sends a SessionCreated message to the requesting Actor.
   *
   * @param username username
   * @param deviceNameOption device name
   * @param replyTo requesting Actor
   */
  private def createSession(username: String, deviceNameOption: Option[String], replyTo: ActorRef[Response]): Unit =
    val ulid = ULID.newULIDString
    val deviceId = ULID.newULIDString
    val token = Token.generateAndSign(ulid)

    // The RETURN value allows to discern whether the MATCH on User succeeds. Otherwise, using
    // execute() would succeed even if the MATCH failed.

    // TODO: Store lastSeenIp and lastSeenAt.
    c"""
      MATCH (u:User)
      WHERE u.name = $username
      CREATE (s:Session {
        ulid: $ulid,
        token: $token
      }),
      (d:Device {
        identifier: $deviceId,
        displayName: $deviceNameOption
      }),
      (u)-[:AUTHENTICATED_AS]->(s)-[:CONNECTED_FROM]->(d)<-[:OWNS]-(u)
      RETURN count(u)
    """
      .query(ResultMapper.int)
      .withResultSummary
      .single(Database.driver)
      .onComplete:
        case Success((userCount, _)) if userCount == 0 =>
          replyTo ! UserNotFound
        // Is checking the results in such detail unnecessary?
        case Success((_, summary)) if summary.counters.nodesCreated == 2 && summary.counters.relationshipsCreated == 3 =>
          replyTo ! SessionCreated(ulid, token, deviceId)
        case Success((_, summary)) =>
          replyTo ! SessionFailed(RuntimeException(s"Created ${summary.counters.nodesCreated} nodes and ${summary.counters.relationshipsCreated} relationships; expected 2 and 3"))
        case Failure(error) =>
          replyTo ! SessionFailed(error)

  /**
   * Updates an existing Session with a new token
   *
   * On success, it sends a SessionCreated message to the requesting Actor.
   *
   * @param ulid Session ULID
   * @param replyTo requesting Actor
   */
  private def updateSession(ulid: String, replyTo: ActorRef[Response]): Unit =
    val token = Token.generateAndSign(ulid)

    // The call to `single()` will error if the Session isn't found, but since this method is
    // called immediately after a successful lookup of the Session, that should never happen. If it
    // does, then it's truly an exceptional condition.

    c"""
      MATCH (s:Session)-[:CONNECTED_FROM]->(d:Device)
      WHERE s.ulid = $ulid
      SET s.token = $token
      RETURN d.identifier
    """
      .query(ResultMapper.string)
      .list(Database.driver)
      .onComplete:
        case Success(deviceId :: _) =>
          replyTo ! SessionCreated(ulid, token, deviceId)
        case Success(Nil) =>
          replyTo ! SessionFailed(RuntimeException("unreachable"))
        case Failure(error) =>
          replyTo ! SessionFailed(error)

  private def deleteSession(ulid: String, replyTo: ActorRef[Response]): Unit =
    c"MATCH (s:Session) WHERE s.ulid = $ulid DETACH DELETE s"
      .execute
      .resultSummary(Database.driver)
      .onComplete:
        case Failure(error) =>
          replyTo ! SessionDeletionFailed(error.getMessage)
        case Success(result) =>
          if result.counters.nodesDeleted == 1 then
            replyTo ! SessionDeleted
          else
            replyTo ! SessionDeletionFailed(s"Session not found with ID $ulid")

  private def deleteAllSessions(username: String, replyTo: ActorRef[Response]): Unit =
    c"""MATCH (u:User)
        WHERE u.name = $username
        MATCH (u)-[:AUTHENTICATED_AS]->(s:Session)
        MATCH (u)-[:OWNS]->(d:Device)
        WHERE u.name = $username
        DETACH DELETE s, d"""
      .execute
      .resultSummary(Database.driver)
      .onComplete:
        case Failure(error) =>
          replyTo ! AllSessionsDeletionFailed(error.getMessage)
        case Success(result) =>
          if result.counters.nodesDeleted > 0 then
            replyTo ! AllSessionsDeleted
          else
            replyTo ! AllSessionsDeletionFailed(s"No Sessions or Devices found with user $username")

  /**
   * Verifies a token by decoding it and then looking up the Session it references
   *
   * JWT-specific verification involves validating its structure, checking that the current time
   * is between its not-before and expires-at claims, and verifying its signature.
   *
   * In addition, this method checks that the JWT's subject claim matches the ULID of an existing
   * Session.
   *
   * @param token JWT to validate
   * @param replyTo requesting Actor
   */
  private def verifyToken(token: String, replyTo: ActorRef[Response]): Unit =
    Token.verify(token) match {
      case Left(error) =>
        replyTo ! TokenFailed(error)

      case Right(decodedJwt) =>
        val ulid = decodedJwt.getSubject

        // Look up Session by ULID
        c"""MATCH (u:User)-[:AUTHENTICATED_AS]->(s:Session)-[:CONNECTED_FROM]->(d:Device)
            WHERE s.ulid = $ulid
            RETURN u, s, d"""
          .query(ResultMapper.tuple[User, Session, Device])
          .list(Database.driver)
          .onComplete:
            case Success((user, session, device) :: _) =>
              replyTo ! TokenPassed(user, session, device)
            case Success(Nil) =>
              replyTo ! TokenFailed(new RuntimeException("Subject invalid: Session not found"))
            case Failure(error) =>
              replyTo ! TokenOtherError(error)
    }
}
