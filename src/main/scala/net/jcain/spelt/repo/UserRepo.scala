package net.jcain.spelt.repo

import net.jcain.spelt.models.{Database, User}
import net.jcain.spelt.service.Auth
import org.neo4j.driver.Values
import org.neo4j.driver.types.Node

import scala.compat.java8.FutureConverters
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

object UserRepo {
  def createUser(identifier: String, password: String, displayName: String, email: String): Either[Throwable, String] = {
    if (userExists(identifier)) {
      Left(new IllegalArgumentException(s"user $identifier already exists"))
    } else {
      val session = Database.getSession

      val completionStage = session.writeTransactionAsync(tx => {
        tx.runAsync(
          """
            CREATE (u:User {
              identifier: $identifier,
              encryptedPassword: $encryptedPassword,
              displayName: $displayName,
              email: $email
            }) RETURN u.identifier
          """,
          Values.parameters(
            "identifier", identifier,
            "encryptedPassword", Auth.hashPassword(password),
            "displayName", displayName,
            "email", email
          )
        )
          .thenCompose(cursor => cursor.nextAsync)
          .thenApply(record => record.get(0).asString)
      })

      val fut = FutureConverters.toScala(completionStage)
      val result = Await.result(fut, 1 minutes)

      session.closeAsync
      Right(result)
    }
  }

  def userExists(identifier: String): Boolean = {
    val session = Database.getSession

    val completionStage = session.readTransactionAsync(tx =>
      tx.runAsync(
        "MATCH (u:User) WHERE u.identifier = $identifier RETURN true",
        Values.parameters("identifier", identifier)
      )
        .thenCompose(cursor => cursor.nextAsync)
        .thenApply(
          recordOrNull => Option(recordOrNull) match {
            case None => false
            case Some(record) => record.get(0).asBoolean(false)
          }
        )
    )
    val fut = FutureConverters.toScala(completionStage)
    val result = Await.result(fut, 1 minutes)

    session.closeAsync
    result
  }

  def getUser(identifier: String): Option[User] = {
    val session = Database.getSession

    val completionStage = session.readTransactionAsync(tx =>
      tx.runAsync(
        "MATCH (u:User) WHERE u.identifier = $identifier RETURN u",
        Values.parameters("identifier", identifier)
      )
        .thenCompose(cursor => cursor.nextAsync)
        .thenApply(
          recordOrNull =>
            Option(recordOrNull).map(record => record.get(0).asNode)
        )
    )
    val fut: Future[Option[Node]] = FutureConverters.toScala(completionStage)
    val result = Await.result(fut, 1 minutes) match {
      case None => None
      case Some(node) =>
        Some(User(
          node.get("identifier").asString,
          node.get("encryptedPassword").asString,
          node.get("displayName").asString,
          node.get("email").asString
        ))
    }

    session.closeAsync
    result
  }
}
