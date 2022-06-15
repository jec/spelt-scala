package net.jcain.spelt.repo

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import net.jcain.spelt.models.{Database, User}
import net.jcain.spelt.service.Auth
import org.neo4j.driver.Values
import org.neo4j.driver.types.Node

import scala.language.postfixOps

object UserRepo {
  sealed trait Request
  sealed trait Response

  case class CreateUser(identifier: String, password: String, email: String, replyTo: ActorRef[Response]) extends Request
  case class CreateUserResponse(result: Either[Throwable, String]) extends Response

  case class GetUser(identifier: String, replyTo: ActorRef[Response]) extends Request
  case class GetUserResponse(user: Option[User]) extends Response

  case class UserInquiry(identifier: String, replyTo: ActorRef[Response]) extends Request
  case class UserInquiryResponse(exists: Boolean) extends Response

  /**
   * Dispatches received messages
   *
   * @return Behaviors
   */
  def apply(): Behavior[Request] = Behaviors.receiveMessage {
    case CreateUser(identifier, password, email, replyTo) =>
      create(identifier, password, email, replyTo)
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
      // TODO: Check for existing user.

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
          "MATCH (u:User) WHERE u.identifier = $identifier RETURN true",
          Values.parameters("identifier", identifier)
        )
        .thenCompose(_.nextAsync)
      )
      .thenApply(Option(_) match {
        case None => false
        case Some(record) => record.get(0).asBoolean(false)
      })
      .thenAccept(replyTo ! UserInquiryResponse(_))
      .whenComplete((_, _) => session.closeAsync)
  }
}
