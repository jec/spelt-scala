package net.jcain.spelt.repo

import net.jcain.spelt.models.Database
import org.neo4j.driver.Values

import java.util.UUID
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
              email: $email,
              uuid: $uuid
            }) RETURN u.uuid
          """,
          Values.parameters(
            "identifier", identifier,
            "encryptedPassword", password,
            "displayName", displayName,
            "email", email,
            "uuid", UUID.randomUUID.toString
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
}
