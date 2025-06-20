package net.jcain.spelt.store

import neotypes.generic.implicits.*
import neotypes.mappers.ResultMapper
import neotypes.syntax.all.*
import net.jcain.spelt.models.{Database, User}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.ExecutionContext.Implicits.global
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
  final case class CreateUser(identifier: String, password: String, email: String, replyTo: ActorRef[Response]) extends Request
  final case class GetUser(identifier: String, replyTo: ActorRef[Response]) extends Request
  final case class UserInquiry(identifier: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateUserResponse(result: Either[Throwable, String]) extends Response
  final case class GetUserResponse(user: Either[Throwable, Option[User]]) extends Response
  final case class UserInquiryResponse(exists: Either[Throwable, Boolean]) extends Response

  /**
   * Dispatches received messages
   *
   * @return the subsequent Behaviors
   */
  def apply(): Behavior[Request] = Behaviors.receiveMessage:
    case CreateUser(identifier, password, email, replyTo) =>
      checkBeforeCreate(identifier, password, email, replyTo)
      Behaviors.same

    case GetUser(identifier, replyTo) =>
      read(identifier, replyTo)
      Behaviors.same

    case UserInquiry(identifier, replyTo) =>
      check(identifier, replyTo)
      Behaviors.same

  /**
   * Creates a user
   *
   * @param identifier local username
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def create(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit =
    c"CREATE (u:User { identifier: $identifier, encryptedPassword: $password, email: $email }) RETURN u.identifier"
      .query(ResultMapper.string)
      .single(Database.driver)
      .onComplete:
        case Success(identifier) =>
          replyTo ! CreateUserResponse(Right(identifier))
        case Failure(error) =>
          replyTo ! CreateUserResponse(Left(error))

  /**
   * Looks up a user by `identifier` and responds with `Some(user)`; else `None`
   *
   * @param identifier username to look up
   * @param replyTo Actor that receives response
   */
  private def read(identifier: String, replyTo: ActorRef[Response]): Unit =
    c"MATCH (u:User) WHERE u.identifier = $identifier RETURN u"
      .query(ResultMapper.productDerive[User])
      .list(Database.driver)
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
   * @param identifier username to look up
   * @param replyTo Actor that receives response
   */
  private def check(identifier: String, replyTo: ActorRef[Response]): Unit =
    c"MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)"
      .query(ResultMapper.int)
      .single(Database.driver)
      .onComplete:
        case Success(count) =>
          replyTo ! UserInquiryResponse(Right(count > 0))
        case Failure(error) =>
          replyTo ! UserInquiryResponse(Left(error))

  /**
   * Looks up a user before calling create()
   *
   * @param identifier username to look up
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def checkBeforeCreate(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit =
    c"MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)"
      .query(ResultMapper.int)
      .single(Database.driver)
      .onComplete:
        case Success(0) =>
          create(identifier,password, email, replyTo)
        case Success(_) =>
          replyTo ! CreateUserResponse(Left(new IllegalArgumentException(s"User \"$identifier\" already exists")))
        case Failure(error) =>
          replyTo ! CreateUserResponse(Left(error))
