package net.jcain.spelt.service

import net.jcain.spelt.models.{Device, Session, User}
import net.jcain.spelt.store.SessionStore
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionBuilder, BodyParser, DefaultPlayBodyParsers, Request, Result, WrappedRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object AuthenticatedAction {
  class AuthenticatedRequest[+A](
    val currentUser: User,
    val currentSession: Session,
    val currentDevice: Device,
    request: Request[A]
  ) extends WrappedRequest[A](request) {
    protected override def newWrapper[B](request: Request[B]): AuthenticatedRequest[B] =
      new AuthenticatedRequest[B](currentUser, currentSession, currentDevice, request)
  }
}

class AuthenticatedAction @Inject()(
  val defaultParsers: DefaultPlayBodyParsers,
  sessionStore: ActorRef[SessionStore.Request]
)(
  implicit val executionContext: ExecutionContext,
  sch: Scheduler
) extends ActionBuilder[AuthenticatedAction.AuthenticatedRequest, JsValue]
    with Logging {
  import AuthenticatedAction.AuthenticatedRequest

  val parser: BodyParser[JsValue] = defaultParsers.json

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    authenticate(request)
      .flatMap {
        case None => Future.successful(Unauthorized)
        case Some(authRequest) =>
          block(authRequest)
      }

  private def authenticate[A](request: Request[A]): Future[Option[AuthenticatedRequest[A]]] =
    implicit val timeout: Timeout = 5.seconds

    request.headers.get("Authorization") match {
      case None =>
        logger.debug("No Authorization header")
        Future.successful(None)

      case Some(authValue) =>
        logger.debug { s"Authorization: $authValue" }

        if authValue.startsWith("Bearer ") then
          sessionStore.ask(ref => SessionStore.VerifyToken(authValue.substring(7), ref)).map {
            case SessionStore.TokenPassed(user, session, device) =>
              Some(AuthenticatedRequest(user, session, device, request))

            case SessionStore.TokenFailed(error) =>
              logger.info { s"Authentication failed: ${error.getMessage}" }
              None

            case SessionStore.TokenOtherError(error) =>
              logger.info { s"Authentication failed: ${error.getMessage}" }
              None
          }
        else
          Future.successful(None)
    }
}
