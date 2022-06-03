package net.jcain.spelt.service

import net.jcain.spelt.models.User
import org.json4s.{DefaultFormats, Formats, JValue}

object Auth {
  // Request classes
  case class Identifier(`type`: String, user: String)
  case class PasswordRequest(device_id: Option[String],
                             identifier: Identifier,
                             initial_device_display_name: Option[String],
                             password: String,
                             `type`: String)

  // Login result classes
  sealed trait Result
  case class Success(user: User) extends Result
  case class Unauthenticated(message: String) extends Result
  case class Failure(message: String) extends Result

  case class MalformedRequestError(private val message: String = "",
                                   private val cause: Throwable = None.orNull) extends Exception(message, cause)

  def logIn(parsedParams: JValue): Result = {
    implicit val jsonFormats: Formats = DefaultFormats

    try {
      parsedParams.extract[PasswordRequest] match {
        case PasswordRequest(
        device_id,
        Identifier("m.id.user", user),
        displayName,
        password,
        "m.login.password"
        ) =>
          val deviceId = device_id.getOrElse(java.util.UUID.randomUUID().toString)
          Success(User())

        case _ =>
          Failure("Request was malformed")
      }
    } catch {
        case error: Throwable =>
          Failure(error.toString)
    }
  }
}
