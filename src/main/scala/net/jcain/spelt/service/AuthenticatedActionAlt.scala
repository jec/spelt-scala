package net.jcain.spelt.service

import net.jcain.spelt.models.User
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, RequestHeader, Result}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

object AuthenticatedActionAlt {
  private def authenticate(request: RequestHeader): Option[User] =
    request.session.get("user").map(User(_, "foo", "bar"))
    
  private def onUnauthorized(_request: RequestHeader): Result =
    Unauthorized(Json.obj("error_message" -> "Authentication failed"))
}

class AuthenticatedActionAlt(parser: BodyParser[AnyContent])(implicit ec: ExecutionContext)
  extends AuthenticatedBuilder[User](
    AuthenticatedActionAlt.authenticate,
    parser,
    AuthenticatedActionAlt.onUnauthorized
  ) {
  @Inject()
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent])
  }
}
