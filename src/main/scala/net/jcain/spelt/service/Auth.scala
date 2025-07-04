package net.jcain.spelt.service

import com.google.inject.Provides
import net.jcain.spelt.models.User
import net.jcain.spelt.store.{SessionStore, UserStore}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import play.api.libs.concurrent.ActorModule
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import play.api.libs.json.Reads.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/**
 * An Actor that implements user authentication
 *
 * Messages it receives:
 * * LogIn -- attempt to log in a user from a login HTTP request
 *   Responses:
 *   * LoginSucceeded
 *   * LoginFailed
 *
 * * LogOut -- log out the authenticated user's session
 *   Responses:
 *   * LogoutSucceeded
 *   * LogoutFailed
 *
 * * LogOutAll -- log out of all the authenticated user's sessions
 *   Responses:
 *   * LogoutAllSucceeded
 *   * LogoutAllFailed
 */
object Auth extends ActorModule {
  type Message = Request

  implicit val timeout: Timeout = 3.seconds

  // Actor messages
  sealed trait Request
  final case class LogIn(parsedParams: JsValue, remoteIpAddress: String, replyTo: ActorRef[Response]) extends Request
  private case class UserFound(user: User, password: String, remoteIpAddress: String, deviceIdOption: Option[String], deviceNameOption: Option[String], replyTo: ActorRef[Response]) extends Request
  private case class UserNotFound(name: String, replyTo: ActorRef[Response]) extends Request
  private case class SessionCreated(name: String, token: String, deviceId: String, replyTo: ActorRef[Response]) extends Request
  private case class OtherFailure(message: String, replyTo: ActorRef[Response]) extends Request
  final case class LogOut(sessionId: String, replyTo: ActorRef[Response]) extends Request
  private final case class SessionDeleted(replyTo: ActorRef[Response]) extends Request
  private final case class SessionNotDeleted(message: String, replyTo: ActorRef[Response]) extends Request
  final case class LogOutAll(username: String, replyTo: ActorRef[Response]) extends Request
  private final case class AllSessionsDeleted(replyTo: ActorRef[Response]) extends Request
  private final case class AllSessionsNotDeleted(message: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class LoginSucceeded(name: String, token: String, deviceId: String) extends Response
  final case class LoginFailed(message: String) extends Response
  object LogoutSucceeded extends Response
  final case class LogoutFailed(message: String) extends Response
  object LogoutAllSucceeded extends Response
  final case class LogoutAllFailed(message: String) extends Response

  // JSON classes
  case class Identifier(`type`: String, user: String)
  case class PasswordLogin(device_id: Option[String],
                           identifier: Identifier,
                           initial_device_display_name: Option[String],
                           password: String,
                           `type`: String)

  // Read a JSON Identifier
  implicit val identifierReads: Reads[Identifier] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "user").read[String]
  )(Identifier.apply _)

  // Read JSON params for a PasswordLogin
  implicit val passwordLoginReads: Reads[PasswordLogin] = (
    (JsPath \ "device_id").readNullable[String] and
      (JsPath \ "identifier").read[Identifier] and
      (JsPath \ "initial_device_display_name").readNullable[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "type").read[String]
  )(PasswordLogin.apply _)

  val argon2Encoder = new Argon2PasswordEncoder(8, 64, 4, 12, 3)

  /**
   * Dispatches received messages
   *
   * @return the subsequent Behaviors
   */
  @Provides
  def apply(
    userStore: ActorRef[UserStore.Request],
    sessionStore: ActorRef[SessionStore.Request]
  ): Behavior[Request] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case LogIn(parsedParams, remoteIpAddress, replyTo) =>
          requestUser(parsedParams, remoteIpAddress, context, userStore, replyTo)
          Behaviors.same

        case UserFound(user, password, remoteIpAddress, deviceIdOption, deviceNameOption, replyTo) =>
          requestSession(user, password, remoteIpAddress, deviceIdOption, deviceNameOption, context, sessionStore, replyTo)
          Behaviors.same

        case UserNotFound(_, replyTo) =>
          replyTo ! LoginFailed("Username or password mismatch")
          Behaviors.same

        case SessionCreated(name, token, deviceId, replyTo) =>
          replyTo ! LoginSucceeded(name, token, deviceId)
          Behaviors.same

        case OtherFailure(message, replyTo) =>
          replyTo ! LoginFailed(message)
          Behaviors.same

        case LogOut(sessionId, replyTo) =>
          requestSessionDeletion(sessionId, context, sessionStore, replyTo)
          Behaviors.same

        case SessionDeleted(replyTo) =>
          replyTo ! LogoutSucceeded
          Behaviors.same

        case SessionNotDeleted(message, replyTo) =>
          replyTo ! LogoutFailed(message)
          Behaviors.same

        case LogOutAll(username, replyTo) =>
          requestAllSessionsDeletion(username, context, sessionStore, replyTo)
          Behaviors.same

        case AllSessionsDeleted(replyTo) =>
          replyTo ! LogoutAllSucceeded
          Behaviors.same

        case AllSessionsNotDeleted(message, replyTo) =>
          replyTo ! LogoutAllFailed(message)
          Behaviors.same
      }
    }

  /**
   * Returns a hashed version of the specified password
   *
   * @param password clear-text password
   *
   * @return hashed password
   */
  def hashPassword(password: String): String = {
    argon2Encoder.encode(password)
  }

  /**
   * Checks whether the specified `plainPassword` hashes to the `encryptedPassword`
   *
   * @param encryptedPassword hashed password
   * @param plainPassword clear-text password
   *
   * @return whether the passwords match
   */
  def passwordMatches(encryptedPassword: String, plainPassword: String): Boolean = {
    argon2Encoder.matches(plainPassword, encryptedPassword)
  }

  /**
   * Receives the payload of the HTTP login request and sends a message to the UserStore to retrieve
   * the User record
   *
   * @param parsedParams payload of HTTP login request
   * @param context Actor context
   * @param replyTo requesting Actor
   */
  private def requestUser(parsedParams: JsValue,
                          remoteIpAddress: String,
                          context: ActorContext[Request],
                          userStore: ActorRef[UserStore.Request],
                          replyTo: ActorRef[Response]): Unit =
    parsedParams.validate[PasswordLogin] match {
      case JsSuccess(PasswordLogin(
        deviceIdOption,
        Identifier("m.id.user", username),
        deviceNameOption,
        password,
        "m.login.password"
      ), _) =>
        implicit val timeout: Timeout = 3.seconds

        // Ask UserStore for the User and translate response to an Auth.Request.
        context.ask(userStore, ref => UserStore.GetUser(username, ref)) {
          case Success(UserStore.GetUserResponse(Right(Some(user)))) =>
            UserFound(user, password, remoteIpAddress, deviceIdOption, deviceNameOption, replyTo)

          case Success(UserStore.GetUserResponse(Right(None))) =>
            UserNotFound(username, replyTo)

          case Success(_) =>
            OtherFailure("unreachable", replyTo)

          case Failure(error) =>
            OtherFailure(error.getMessage, replyTo)
        }

      case _ =>
        replyTo ! LoginFailed("Request was malformed")
    }

  /**
   * Processes a successful response from the UserStore and verifies the password
   *
   * If the password matches, a message is sent to the SessionStore to create a Session. Else a
   * failure message is sent to the original requesting Actor.
   *
   * @param user matching User record
   * @param password password from the login request
   * @param deviceNameOption initial device name
   * @param replyTo requesting Actor
   */
  private def requestSession(user: User,
                             password: String,
                             remoteIpAddress: String,
                             deviceIdOption: Option[String],
                             deviceNameOption: Option[String],
                             context: ActorContext[Request],
                             sessionStore: ActorRef[SessionStore.Request],
                             replyTo: ActorRef[Response]): Unit = {
    implicit val timeout: Timeout = 3.seconds

    if (passwordMatches(user.encryptedPassword, password)) {
      context.ask(sessionStore, ref => SessionStore.GetOrCreateSession(user.name, remoteIpAddress, deviceIdOption, deviceNameOption, ref)) {
        case Success(SessionStore.SessionCreated(ulid, token, deviceId)) =>
          SessionCreated(user.name, token, deviceId, replyTo)
        case Success(SessionStore.SessionFailed(error)) =>
          OtherFailure(error.getMessage, replyTo)
        case Success(_) =>
          OtherFailure("unreachable", replyTo)
        case Failure(error) =>
          OtherFailure(error.getMessage, replyTo)
      }
    } else {
     replyTo ! LoginFailed("Username or password mismatch")
    }
  }

  private def requestSessionDeletion(token: String,
                                     context: ActorContext[Request],
                                     sessionStore: ActorRef[SessionStore.Request],
                                     replyTo: ActorRef[Auth.Response]): Unit =
    implicit val timeout: Timeout = 3.seconds

    context.ask(sessionStore, ref => SessionStore.DeleteSession(token, ref)) {
      case Success(SessionStore.SessionDeleted) =>
        SessionDeleted(replyTo)
      case Success(SessionStore.SessionDeletionFailed(message)) =>
        SessionNotDeleted(message, replyTo)
    }

  private def requestAllSessionsDeletion(username: String,
                                         context: ActorContext[Request],
                                         sessionStore: ActorRef[SessionStore.Request],
                                         replyTo: ActorRef[Auth.Response]): Unit =
    context.ask(sessionStore, ref => SessionStore.DeleteAllSessions(username, ref)) {
      case Success(SessionStore.AllSessionsDeleted) =>
        AllSessionsDeleted(replyTo)
      case Success(SessionStore.AllSessionsDeletionFailed(message)) =>
        AllSessionsNotDeleted(message, replyTo)
    }
}
