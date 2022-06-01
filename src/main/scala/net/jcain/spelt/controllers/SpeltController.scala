package net.jcain.spelt.controllers

import akka.actor.ActorSystem
import net.jcain.spelt.models.Database
import org.json4s.{DefaultFormats, Formats}
import org.neo4j.driver.Values
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import java.net.URLDecoder
import scala.compat.java8.FutureConverters
import scala.concurrent.{ExecutionContext, Future}

case class Message(message: String)

class SpeltController(system: ActorSystem) extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  protected implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
  }

  get("/:message") {
    val message = params("message")

    try {
      val completionStage = Database.getSession.writeTransactionAsync(tx => {
        tx.runAsync(
          "CREATE (a:Greeting) SET a.message = $message RETURN a.message",
          Values.parameters("message", message)
        ).thenCompose(cursor => {
          cursor.singleAsync()
        }).thenApply(record => {
          record.get(0).asString()
        }).thenApply(str => {
          Message(URLDecoder.decode(str, "utf8"))
        })
      })
      FutureConverters.toScala(completionStage)
    } catch {
      case error: Throwable =>
        log("error caught", error)
        Future { Message(error.getMessage) }
    }
  }
}
