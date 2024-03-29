package net.jcain.spelt.repo

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import net.jcain.spelt.models.Database
import net.jcain.spelt.service.Token
import org.neo4j.driver.Values

import java.util.UUID

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
 *   * Valid
 *   * Invalid
 *
 * Rules:
 *   - A new login request for the same user and device invalidates any
 *     previous token issued for that device.
 *   - A login request with no device ID generates a new device ID.
 */
object SessionRepo {
  sealed trait Request
  final case class GetOrCreateSession(identifier: String, deviceId: Option[String], deviceName: Option[String], replyTo: ActorRef[Response]) extends Request
  final case class ValidateToken(token: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class SessionCreated(token: String, deviceId: String) extends Response
  final case class SessionFailed(error: Throwable) extends Response
  object Valid extends Response
  final case class Invalid(error: Throwable) extends Response

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
  private def readByDevice(identifier: String, deviceId: String, deviceName: Option[String], replyTo: ActorRef[Response]): Unit = {
    val dbSession = Database.getSession

    dbSession.executeReadAsync(
      _.runAsync(
        """
          MATCH (u:User)-[AUTHENTICATED_AS]->(s:Session)
          WHERE u.identifier = $identifier
          AND s.deviceId = $deviceId
          RETURN s.uuid
        """,
        Values.parameters(
          "identifier", identifier,
          "deviceId", deviceId
        )
      )
      .thenCompose(_.nextAsync)
    )
    .thenAccept(Option(_) match {
      // If a matching Session is found, update it.
      case Some(record) =>
        updateSession(record.get(0).asString, replyTo)

      // else create one.
      case None =>
        createSession(identifier, deviceName, replyTo)
    })
    .whenComplete((_, _) => dbSession.closeAsync)
  }

  /**
   * Creates a new Session with a new token and device ID
   *
   * On successful creation, it sends a SessionCreated message to the requesting Actor.
   *
   * @param identifier user name
   * @param deviceName device name
   * @param replyTo requesting Actor
   */
  private def createSession(identifier: String, deviceName: Option[String], replyTo: ActorRef[Response]): Unit = {
    val dbSession = Database.getSession
    val uuid = UUID.randomUUID.toString
    val deviceId = UUID.randomUUID.toString
    val token = Token.generateAndSign(uuid)

    dbSession.executeWriteAsync(
      _.runAsync(
    """
          MATCH (u:User)
          WHERE u.identifier = $identifier
          CREATE (u)-[:AUTHENTICATED_AS]->(s:Session {
            uuid: $uuid,
            deviceId: $deviceId,
            token: $token,
            deviceName: $deviceName
          })
          RETURN s.deviceId
        """,
        Values.parameters(
          "identifier", identifier,
          "uuid", uuid,
          "deviceId", deviceId,
          "token", token,
          "deviceName", deviceName.getOrElse("")
        )
      )
      .thenCompose(_.nextAsync)
    )
    .thenApply(record => SessionCreated(token, record.get(0).asString).asInstanceOf[Response])
    .exceptionally(error => SessionFailed(error).asInstanceOf[Response])
    .thenAccept(replyTo ! _)
    .whenComplete((_, _) => dbSession.closeAsync)
  }

  /**
   * Updates an existing Session with a new token and sends a SessionCreated message to the requesting Actor
   *
   * @param uuid Session UUID
   * @param replyTo requesting Actor
   */
  private def updateSession(uuid: String, replyTo: ActorRef[Response]): Unit = {
    val dbSession = Database.getSession
    val token = Token.generateAndSign(uuid)

    dbSession.executeWriteAsync(
      _.runAsync(
    """
          MATCH (s:Session)
          WHERE s.uuid = $uuid
          WITH s
          SET s.token = $token
          RETURN s.deviceId
        """,
        Values.parameters(
          "uuid", uuid,
          "token", token
        )
      )
      .thenCompose(_.nextAsync)
    )
    .thenAccept(Option(_) match {
      case Some(record) =>
        replyTo ! SessionCreated(token, record.get(0).asString)
      case None =>
        // TODO: Send something here.
    })
    .whenComplete((_, _) => dbSession.closeAsync)
  }

  /**
   * Validates a token by decoding it and then looking up the Session it references
   *
   * @param token JWT to validate
   * @param replyTo requesting Actor
   */
  private def validateToken(token: String, replyTo: ActorRef[Response]): Unit = {
    Token.verify(token) match {
      case Left(error) =>
        replyTo ! Invalid(error)

      case Right(decodedJwt) =>
        val uuid = decodedJwt.getSubject
        val dbSession = Database.getSession

        // Look up Session by Uuid
        dbSession.executeReadAsync(
          _.runAsync(
            "MATCH (s:Session) WHERE s.uuid = $uuid RETURN count(s)",
            Values.parameters("uuid", uuid)
          )
          .thenCompose(_.nextAsync)
        )
        .thenApply(record =>
          if (record.get(0).asInt == 1)
            replyTo ! Valid
          else
            replyTo ! Invalid(new RuntimeException("Subject invalid: Session not found"))
        )
        .whenComplete((_, _) => dbSession.closeAsync)
    }
  }
}
