package net.jcain.spelt.store

import neotypes.AsyncDriver
import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.User
import net.jcain.spelt.service.Auth
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * An Actor that implements the CRUD operations for User nodes
 *
 * Messages it receives:
 * * CreateUser -- create a User node
 *   Responses:
 *   * CreateUserResponse -- either the fully qualified user identifier or a Throwable
 *
 * * GetUser -- retrieve a User node
 *   Responses:
 *   * GetUserResponse -- either a User record or None
 *
 * * UserInquiry -- does a user identifier exist?
 *   Responses:
 *   * UserInquiryResponse -- contains a boolean indicating existence
 */
object UserStore:
  sealed trait Request
  final case class CreateUser(name: String, password: String, email: String, replyTo: ActorRef[Response]) extends Request
  final case class GetUser(name: String, replyTo: ActorRef[Response]) extends Request
  final case class UserInquiry(name: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateUserResponse(result: Either[Throwable, String]) extends Response
  final case class GetUserResponse(user: Either[Throwable, Option[User]]) extends Response
  final case class UserInquiryResponse(exists: Either[Throwable, Boolean]) extends Response

  @Inject()
  def apply()(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Behavior[Request] =
    Behaviors.receiveMessage:
      case CreateUser(name, password, email, replyTo) =>
        checkBeforeCreate(name, password, email, replyTo)
        Behaviors.same

      case GetUser(name, replyTo) =>
        read(name, replyTo)
        Behaviors.same

      case UserInquiry(name, replyTo) =>
        check(name, replyTo)
        Behaviors.same

  /**
   * Creates a user
   *
   * @param name local username
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def create(name: String, password: String, email: String, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit =
    val encryptedPassword = Auth.argon2Encoder.encode(password)

    c"CREATE (u:User { name: $name, encryptedPassword: $encryptedPassword, email: $email }) RETURN u.name"
      .query(ResultMapper.string)
      .single(driver)
      .onComplete:
        case Success(name) =>
          replyTo ! CreateUserResponse(Right(name))
        case Failure(error) =>
          replyTo ! CreateUserResponse(Left(error))

  /**
   * Looks up a user by `name` and responds with `Some(user)`; else `None`
   *
   * @param name username to look up
   * @param replyTo Actor that receives response
   */
  private def read(name: String, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit =
    c"MATCH (u:User) WHERE u.name = $name RETURN u"
      .query(ResultMapper.productDerive[User])
      .list(driver)
      .onComplete:
        case Success(user :: _) =>
          replyTo ! GetUserResponse(Right(Some(user)))
        case Success(Nil) =>
          replyTo ! GetUserResponse(Right(None))
        case Failure(error) =>
          replyTo ! GetUserResponse(Left(error))

  /**
   * Looks up a user and sends a message to the requester indicating existence
   *
   * @param name username to look up
   * @param replyTo Actor that receives response
   */
  private def check(name: String, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit =
    c"MATCH (u:User) WHERE u.name = $name RETURN count(u)"
      .query(ResultMapper.int)
      .single(driver)
      .onComplete:
        case Success(count) =>
          replyTo ! UserInquiryResponse(Right(count > 0))
        case Failure(error) =>
          replyTo ! UserInquiryResponse(Left(error))

  /**
   * Looks up a user before calling create()
   *
   * @param name username to look up
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def checkBeforeCreate(name: String, password: String, email: String, replyTo: ActorRef[Response])(implicit driver: AsyncDriver[Future], xc: ExecutionContext): Unit =
    c"MATCH (u:User) WHERE u.name = $name RETURN count(u)"
      .query(ResultMapper.int)
      .single(driver)
      .onComplete:
        case Success(0) =>
          create(name, password, email, replyTo)
        case Success(_) =>
          replyTo ! CreateUserResponse(Left(new IllegalArgumentException(s"User \"$name\" already exists")))
        case Failure(error) =>
          replyTo ! CreateUserResponse(Left(error))
