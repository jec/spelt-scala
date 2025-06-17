package net.jcain.spelt.repo

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import net.jcain.spelt.models.{Database, User}
import net.jcain.spelt.service.Auth
import org.neo4j.driver.Values
import org.neo4j.driver.exceptions.NoSuchRecordException
import org.neo4j.driver.types.Node

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.FutureConverters
import scala.language.postfixOps
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
object UserRepo:
  sealed trait Request
  final case class CreateUser(identifier: String, password: String, email: String, replyTo: ActorRef[Response]) extends Request
  final case class GetUser(identifier: String, replyTo: ActorRef[Response]) extends Request
  final case class UserInquiry(identifier: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class CreateUserResponse(result: Either[Throwable, String]) extends Response
  final case class GetUserResponse(user: Option[User]) extends Response
  final case class UserInquiryResponse(exists: Boolean) extends Response

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
   * @param identifier local user name
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def create(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit =
    val dbSession = Database.getSession

    val cypher = """
      CREATE (u:User {
        identifier: $identifier,
        encryptedPassword: $encryptedPassword,
        email: $email
      }) RETURN u.identifier"""

    val bindings = Values.parameters(
      "identifier", identifier,
      "encryptedPassword", Auth.hashPassword(password),
      "email", email
    )

    FutureConverters
      .CompletionStageOps(
        dbSession
          .executeWriteAsync(_.runAsync(cypher, bindings).thenCompose(_.singleAsync))
          .thenApply(_.get(0).asString)
      )
      .asScala
      .onComplete:
        case Success(identifier) =>
          replyTo ! CreateUserResponse(Right(identifier))
          dbSession.closeAsync
        case Failure(error) =>
          replyTo ! CreateUserResponse(Left(error))
          dbSession.closeAsync

  /**
   * Looks up a user by `identifier` and responds with `Some(user)`; else  `None`
   *
   * @param identifier user name to look up
   * @param replyTo Actor that receives response
   */
  private def read(identifier: String, replyTo: ActorRef[Response]): Unit =
    val dbSession = Database.getSession
    val cypher = "MATCH (u:User) WHERE u.identifier = $identifier RETURN u"
    val bindings = Values.parameters("identifier", identifier)

    FutureConverters
      .CompletionStageOps(
        dbSession
          .executeReadAsync(
            _.runAsync(cypher, bindings)
              .thenCompose(_.singleAsync)
              .exceptionally(error =>
                if error.isInstanceOf[NoSuchRecordException] then
                  println(s"ErrorA: $error")
                  null
                else
                  println(s"ErrorB: $error")
                  null
              )
          )
          .thenApply(Option(_).map(_.get(0).asNode))
          .exceptionally(error =>
            if error.isInstanceOf[NoSuchRecordException] then
              println(s"Error0: $error")
              None
            else
              println(s"Error1: $error")
              None
          )
      )
      .asScala
      .onComplete:
        case Success(Some(node: Node)) =>
          replyTo ! GetUserResponse(Some(User(
            node.get("identifier").asString,
            node.get("encryptedPassword").asString,
            node.get("email").asString
          )))
          dbSession.closeAsync
        case Success(None) =>
          replyTo ! GetUserResponse(None)
          dbSession.closeAsync
        case Failure(error) =>
          // TODO: Handle error.
          println(s"Error2: $error")
          replyTo ! GetUserResponse(None)
          dbSession.closeAsync
  end read

  /**
   * Looks up a user and sends a message to the requester indicating existence
   *
   * @param identifier user name to look up
   * @param replyTo Actor that receives response
   */
  private def check(identifier: String, replyTo: ActorRef[Response]): Unit =
    val dbSession = Database.getSession
    val cypher = "MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)"
    val bindings = Values.parameters("identifier", identifier)

    FutureConverters
      .CompletionStageOps(
        dbSession
        .executeReadAsync(_.runAsync(cypher, bindings).thenCompose(_.singleAsync))
        .thenApply(_.get(0).asInt)
      )
      .asScala
      .onComplete:
        case Success(count) =>
          replyTo ! UserInquiryResponse(count > 0)
          dbSession.closeAsync
        case Failure(error) =>
          // TODO: Handle error.
          dbSession.closeAsync

  /**
   * Looks up a user before calling create()
   *
   * @param identifier user name to look up
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def checkBeforeCreate(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit =
    val dbSession = Database.getSession
    val cypher = "MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)"
    val bindings = Values.parameters("identifier", identifier)

    FutureConverters
      .CompletionStageOps(
        dbSession
        .executeReadAsync(_.runAsync(cypher, bindings).thenCompose(_.singleAsync))
        .thenApply(_.get(0).asInt)
      )
      .asScala
      .onComplete:
        case Success(0) =>
          create(identifier,password, email, replyTo)
          dbSession.closeAsync
        case Success(_) =>
          replyTo ! CreateUserResponse(Left(new IllegalArgumentException(s"User \"$identifier\" already exists")))
          dbSession.closeAsync
        case Failure(error) =>
          // TODO: Handle error.
          dbSession.closeAsync
