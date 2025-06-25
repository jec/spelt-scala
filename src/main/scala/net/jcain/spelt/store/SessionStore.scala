package net.jcain.spelt.store

import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.Database
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
  final case class VerifyToken(token: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class SessionCreated(token: String, deviceId: String) extends Response
  object UserNotFound extends Response
  final case class SessionFailed(error: Throwable) extends Response
  object TokenPassed extends Response
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
      CREATE (u)-[:AUTHENTICATED_AS]->(s:Session {
        ulid: $ulid,
        token: $token
      })-[:CONNECTED_FROM]->(d:Device {
        identifier: $deviceId,
        displayName: $deviceNameOption
      })
      RETURN d.identifier
    """
      .query(ResultMapper.string)
      .list(Database.driver)
      .onComplete:
        case Success(_ :: _) =>
          replyTo ! SessionCreated(token, deviceId)
        case Success(Nil) =>
          replyTo ! UserNotFound
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
          replyTo ! SessionCreated(token, deviceId)
        case Success(Nil) =>
          replyTo ! SessionFailed(RuntimeException("unreachable"))
        case Failure(error) =>
          replyTo ! SessionFailed(error)

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
        c"MATCH (s:Session) WHERE s.ulid = $ulid RETURN count(s)"
          .query(ResultMapper.int)
          .single(Database.driver)
          .onComplete:
            case Success(1) =>
              replyTo ! TokenPassed
            case Success(_) =>
              replyTo ! TokenFailed(new RuntimeException("Subject invalid: Session not found"))
            case Failure(error) =>
              replyTo ! TokenOtherError(error)
    }
}
