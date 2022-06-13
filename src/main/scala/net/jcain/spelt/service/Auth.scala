package net.jcain.spelt.service

import net.jcain.spelt.models.User
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.libs.json.Reads._

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

  implicit val identifierReads: Reads[Identifier] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "user").read[String]
  )(Identifier.apply _)

  implicit val passwordRequestReads: Reads[PasswordRequest] = (
    (JsPath \ "device_id").readNullable[String] and
      (JsPath \ "identifier").read[Identifier] and
      (JsPath \ "initial_device_display_name").readNullable[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "type").read[String]
  )(PasswordRequest.apply _)

  val argon2Encoder = new Argon2PasswordEncoder(8, 64, 4, 12, 3)

  def logIn(parsedParams: JsValue): Result = {
    try {
      parsedParams.validate[PasswordRequest] match {
        case JsSuccess(PasswordRequest(
          deviceIdOption,
          Identifier("m.id.user", username),
          displayName,
          password,
          "m.login.password"
        ), _) =>
          val deviceId = deviceIdOption.getOrElse(java.util.UUID.randomUUID().toString)
          Success(username, Token.generateAndSign(UUID.randomUUID.toString), deviceId)

        case _ =>
          Failure("Request was malformed")
      }
    } catch {
        // TODO: no need for try/catch
        case error: Throwable =>
          Failure(error.toString)
    }
  }

  def hashPassword(password: String): String = {
    argon2Encoder.encode(password)
  }

  def passwordMatches(encryptedPassword: String, plainPassword: String): Boolean = {
    argon2Encoder.matches(plainPassword, encryptedPassword)
  }
}
