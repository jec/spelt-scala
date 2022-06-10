package net.jcain.spelt.repo

import net.jcain.spelt.models.Database
import org.neo4j.driver.Values

import scala.compat.java8.FutureConverters
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

object UserRepo {
//  def createUser(identifier: String, password: String, displayName: String, email: String): String = {
//    val session = Database.getSession
//
//    session.writeTransactionAsync(tx => {
//      tx.runAsync(
//        "CREATE (a:Greeting) SET a.message = $message RETURN a.message",
//        Values.parameters("message", message)
//      ).single().get(0).asString()
//    })
//
//  }

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
