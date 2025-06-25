package net.jcain.spelt.service

import net.jcain.spelt.models.{Device, Session, User}
import net.jcain.spelt.store.SessionStore
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionBuilder, ActionTransformer, AnyContent, BodyParsers, Request, Result, WrappedRequest}

import java.time.ZonedDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

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
  sessionStore: ActorRef[SessionStore.Request],
  val parser: BodyParsers.Default
)(
  implicit val executionContext: ExecutionContext,
  sch: Scheduler
) extends ActionBuilder[AuthenticatedAction.AuthenticatedRequest, JsValue]
    /* with ActionTransformer[Request, AuthenticatedAction1.AuthenticatedRequest] */ {
  import AuthenticatedAction.AuthenticatedRequest

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]) =
    authenticate(request)
      .map { authRequest => block(authRequest) }
      .getOrElse {
        Future.successful(Unauthorized(Json.obj("error_message" -> "Authentication failed")))
      }

  private def authenticate[A](request: Request[A]): Future[AuthenticatedRequest[A]] =
    implicit val timeout: Timeout = 5.seconds

    request.headers.get("Authorization") match {
      case None =>
        Future.failed(RuntimeException("Forbidden"))

      case Some(token) =>
        sessionStore.ask(ref => SessionStore.VerifyToken(token, ref)).map {
          case Success(SessionStore.TokenPassed) =>
            // TODO: TokenPassed should provide these.
            val user = User("foo", "bar", "baz")
            val session = Session("foo", "bar")
            val device = Device("foo", None, "baz", ZonedDateTime.now)
            AuthenticatedRequest(user, session, device, request)
        }
    }

  /*
  def transform[A](
    request: Request[A]
  ): Future[AuthenticatedRequest[A]] =
    implicit val timeout: Timeout = 3.seconds

    request.headers.get("Authorization") match {
      case None =>
        Future.failed(RuntimeException("Forbidden"))

      case Some(token) =>
        sessionStore.ask(ref => SessionStore.VerifyToken(token, ref)).map {
          case Success(SessionStore.TokenPassed) =>
            // TODO: TokenPassed should provide these.
            val user = User("foo", "bar", "baz")
            val session = Session("foo", "bar")
            val device = Device("foo", None, "baz", ZonedDateTime.now)
            AuthenticatedRequest(user, session, device, request)
        }
    }
  */
}
