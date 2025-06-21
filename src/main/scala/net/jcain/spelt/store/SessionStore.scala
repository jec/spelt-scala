package net.jcain.spelt.store

import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.Database
import net.jcain.spelt.service.Token
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * An Actor that implements the CRUD operations for Session nodes
 *
 * Messages it receives:
 * * GetOrCreateSession -- gets (if `identifier` and `deviceId` are found) or creates a Session node
 *   Responses:
 *   * SessionCreated -- includes a JWT and a (possibly new) deviceId
 *   * SessionFailed -- includes a Throwable
 *
 * * ValidateToken -- check whether a `token` references a valid Session node
 *   Responses:
 *   * TokenValid
 *   * TokenInvalid
 *
 * Rules:
 *   - A new login request for the same user and device invalidates any previous token issued for
 *     that device.
 *   - A login request with no device ID generates a new device ID.
 */
object SessionStore {
  sealed trait Request
  final case class GetOrCreateSession(identifier: String,
                                      deviceId: Option[String],
                                      deviceName: Option[String],
                                      replyTo: ActorRef[Response]) extends Request
  final case class ValidateToken(token: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class SessionCreated(token: String, deviceId: String) extends Response
  object UserNotFound extends Response
  final case class SessionFailed(error: Throwable) extends Response
  object TokenValid extends Response
  final case class TokenInvalid(error: Throwable) extends Response

  /**
   * Dispatches received messages
   *
   * @return subsequent Behaviors
   */
  def apply(): Behavior[Request] = Behaviors.receiveMessage {
    case GetOrCreateSession(identifier, deviceIdOption, deviceNameOption, replyTo) =>
      deviceIdOption match {
        // If deviceId isn't specified, create a new Session.
        case None =>
          createSession(identifier, deviceNameOption, replyTo)

        // Else look up Session.
        case Some(deviceId) =>
          readByDevice(identifier, deviceId, deviceNameOption, replyTo)
      }

      Behaviors.same

    case ValidateToken(token, replyTo) =>
      validateToken(token, replyTo)
      Behaviors.same
  }

  /**
   * Looks up Session by `identifier` and `deviceId`; if found then updates that Session with a new token; else creates
   * a new Session
   *
   * @param identifier user name
   * @param deviceId a pre-existing device ID (optional)
   * @param deviceName a device name to use (optional)
   * @param replyTo requesting Actor
   */
  private def readByDevice(identifier: String, deviceId: String, deviceName: Option[String], replyTo: ActorRef[Response]): Unit =
    c"""
      MATCH (u:User)-[AUTHENTICATED_AS]->(s:Session)
      WHERE u.identifier = $identifier
      AND s.deviceId = $deviceId
      RETURN s.uuid
    """
      .query(ResultMapper.string)
      .list(Database.driver)
      .onComplete:
        case Success(uuid :: _) =>
          updateSession(uuid, replyTo)
        case Success(Nil) =>
          createSession(identifier, deviceName, replyTo)
        case Failure(error) =>
          replyTo ! SessionFailed(error)

  /**
   * Creates a new Session with a new token and device ID
   *
   * On successful creation, it sends a SessionCreated message to the requesting Actor.
   *
   * @param identifier user name
   * @param deviceName device name
   * @param replyTo requesting Actor
   */
  private def createSession(identifier: String, deviceName: Option[String], replyTo: ActorRef[Response]): Unit =
    val uuid = UUID.randomUUID.toString
    val deviceId = UUID.randomUUID.toString
    val token = Token.generateAndSign(uuid)

    c"""
      MATCH (u:User)
      WHERE u.identifier = $identifier
      CREATE (u)-[:AUTHENTICATED_AS]->(s:Session {
        uuid: $uuid,
        deviceId: $deviceId,
        token: $token,
        deviceName: $deviceName
      })
      RETURN s.deviceId
    """
      .query(ResultMapper.string)
      .list(Database.driver)
      .onComplete:
        case Success(deviceId :: _) =>
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
   * @param uuid Session UUID
   * @param replyTo requesting Actor
   */
  private def updateSession(uuid: String, replyTo: ActorRef[Response]): Unit =
    val token = Token.generateAndSign(uuid)

    // The call to `single()` will error if the Session isn't found, but since this method is
    // called immediately after a successful lookup of the Session, that should never happen. If it
    // does, then it's truly an exceptional condition.

    c"""
      MATCH (s:Session)
      WHERE s.uuid = $uuid
      WITH s
      SET s.token = $token
      RETURN s.deviceId
    """
      .query(ResultMapper.string)
      .single(Database.driver)
      .onComplete:
        case Success(deviceId) =>
          replyTo ! SessionCreated(token, deviceId)
        case Failure(error) =>
          replyTo ! SessionFailed(error)

  /**
   * Validates a token by decoding it and then looking up the Session it references
   *
   * @param token JWT to validate
   * @param replyTo requesting Actor
   */
  private def validateToken(token: String, replyTo: ActorRef[Response]): Unit =
    Token.verify(token) match {
      case Left(error) =>
        replyTo ! TokenInvalid(error)

      case Right(decodedJwt) =>
        val uuid = decodedJwt.getSubject

        // Look up Session by Uuid
        c"MATCH (s:Session) WHERE s.uuid = $uuid RETURN count(s)"
          .query(ResultMapper.int)
          .single(Database.driver)
          .onComplete:
            case Success(1) =>
              replyTo ! TokenValid
            case Success(_) =>
              replyTo ! TokenInvalid(new RuntimeException("Subject invalid: Session not found"))
            case Failure(error) =>
              () // TODO: Handle error.
    }
}
