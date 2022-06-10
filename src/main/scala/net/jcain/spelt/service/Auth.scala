package net.jcain.spelt.service

import net.jcain.spelt.models.User
import org.json4s.{DefaultFormats, Formats, JValue}

import java.util.UUID

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
  case class Success(userId: String, jwt: String, deviceId: String) extends Result
  case class Unauthenticated(message: String) extends Result
  case class Failure(message: String) extends Result

  case class MalformedRequestError(private val message: String = "",
                                   private val cause: Throwable = None.orNull) extends Exception(message, cause)

  def logIn(parsedParams: JValue): Result = {
    implicit val jsonFormats: Formats = DefaultFormats

    try {
      parsedParams.extract[PasswordRequest] match {
        case PasswordRequest(
          deviceIdOption,
          Identifier("m.id.user", username),
          displayName,
          password,
          "m.login.password"
        ) =>
          val deviceId = deviceIdOption.getOrElse(java.util.UUID.randomUUID().toString)
          Success(username, Token.generateAndSign(UUID.randomUUID.toString), deviceId)

        case _ =>
          Failure("Request was malformed")
      }
    } catch {
        case error: Throwable =>
          Failure(error.toString)
    }
  }
}
