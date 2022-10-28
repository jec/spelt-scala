package net.jcain.spelt.repo

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import net.jcain.spelt.models.{Database, User}
import net.jcain.spelt.service.Auth
import org.neo4j.driver.Values
import org.neo4j.driver.types.Node

import scala.language.postfixOps

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
object UserRepo {
  sealed trait Request
  case class CreateUser(identifier: String, password: String, email: String, replyTo: ActorRef[Response]) extends Request
  case class GetUser(identifier: String, replyTo: ActorRef[Response]) extends Request
  case class UserInquiry(identifier: String, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  case class CreateUserResponse(result: Either[Throwable, String]) extends Response
  case class GetUserResponse(user: Option[User]) extends Response
  case class UserInquiryResponse(exists: Boolean) extends Response

  /**
   * Dispatches received messages
   *
   * @return the subsequent Behaviors
   */
  def apply(): Behavior[Request] = Behaviors.receiveMessage {
    case CreateUser(identifier, password, email, replyTo) =>
      checkBeforeCreate(identifier, password, email, replyTo)
      Behaviors.same

    case GetUser(identifier, replyTo) =>
      read(identifier, replyTo)
      Behaviors.same

    case UserInquiry(identifier, replyTo) =>
      check(identifier, replyTo)
      Behaviors.same
  }

  /**
   * Creates a user
   *
   * @param identifier local user name
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def create(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit = {
      val session = Database.getSession

      session
        .writeTransactionAsync(
          _.runAsync(
            """
              CREATE (u:User {
                identifier: $identifier,
                encryptedPassword: $encryptedPassword,
                email: $email
              }) RETURN u.identifier
            """,
            Values.parameters(
              "identifier", identifier,
              "encryptedPassword", Auth.hashPassword(password),
              "email", email
            )
          )
          .thenCompose(_.nextAsync)
        )
        .thenApply(_.get(0).asString)
        .thenApply(identifier => Right[Throwable, String](identifier).asInstanceOf[Either[Throwable, String]])
        .exceptionally(error => Left[Throwable, String](error).asInstanceOf[Either[Throwable, String]])
        .thenAccept(either => replyTo ! CreateUserResponse(either))
        .whenComplete((_, _) => session.closeAsync)
  }

  /**
   * Looks up a user by `identifier` and responds with `Some(user)`; else  `None`
   *
   * @param identifier user name to look up
   * @param replyTo Actor that receives response
   */
  private def read(identifier: String, replyTo: ActorRef[Response]): Unit = {
    val session = Database.getSession

    session
      .readTransactionAsync(
        _.runAsync(
          "MATCH (u:User) WHERE u.identifier = $identifier RETURN u",
          Values.parameters("identifier", identifier)
        )
        .thenCompose(_.nextAsync)
      )
      .thenApply(Option(_).map(_.get(0).asNode))
      .thenAccept {
        case None =>
          replyTo ! GetUserResponse(None)
        case Some(node: Node) =>
          replyTo ! GetUserResponse(Some(User(
            node.get("identifier").asString,
            node.get("encryptedPassword").asString,
            node.get("email").asString
          )))
      }
      .whenComplete((_, _) => session.closeAsync)
  }

  /**
   * Looks up a user and sends a message to the requester indicating existence
   *
   * @param identifier user name to look up
   * @param replyTo Actor that receives response
   */
  private def check(identifier: String, replyTo: ActorRef[Response]): Unit = {
    val session = Database.getSession

    session
      .readTransactionAsync(
        _.runAsync(
          "MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)",
          Values.parameters("identifier", identifier)
        )
        .thenCompose(_.nextAsync)
      )
      .thenApply(Option(_) match {
        // There should always be exactly 1 record, so this is overly
        // defensive.
        case None => 0
        case Some(record) => record.get(0).asInt
      })
      .thenAccept((x: Int) => replyTo ! UserInquiryResponse(x > 0))
      .whenComplete((_, _) => session.closeAsync)
  }

  /**
   * Looks up a user before calling create()
   *
   * @param identifier user name to look up
   * @param password password
   * @param email email address
   * @param replyTo Actor that receives response
   */
  private def checkBeforeCreate(identifier: String, password: String, email: String, replyTo: ActorRef[Response]): Unit = {
    val session = Database.getSession

    session
      .readTransactionAsync(
        _.runAsync(
          "MATCH (u:User) WHERE u.identifier = $identifier RETURN count(u)",
          Values.parameters("identifier", identifier)
        )
        .thenCompose(_.nextAsync)
      )
      .thenApply(Option(_) match {
        case None =>
          // There should always be exactly 1 record, so this is overly
          // defensive.
          create(identifier,password, email, replyTo)
        case Some(record) =>
          if (record.get(0).asInt == 0)
            create(identifier,password, email, replyTo)
          else
            replyTo ! CreateUserResponse(Left(new IllegalArgumentException(s"User \"$identifier\" already exists")))
      })
      .whenComplete((_, _) => session.closeAsync)
  }
}
