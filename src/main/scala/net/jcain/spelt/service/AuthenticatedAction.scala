package net.jcain.spelt.service

import net.jcain.spelt.models.{Device, Session, User}
import net.jcain.spelt.store.SessionStore
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.mvc.{ActionBuilder, ActionTransformer, AnyContent, BodyParsers, Request, WrappedRequest}

import java.time.ZonedDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AuthenticatedAction {
  class AuthenticatedRequest[A](
    val user: User,
    val session: Session,
    val device: Device,
    request: Request[A]
  ) extends WrappedRequest[A](request)
}

class AuthenticatedAction @Inject() (
  sessionStore: ActorRef[SessionStore.Request],
  val parser: BodyParsers.Default
)(
  implicit xc: ExecutionContext,
  sch: Scheduler
) extends ActionBuilder[AuthenticatedAction.AuthenticatedRequest, AnyContent]
    with ActionTransformer[Request, AuthenticatedAction.AuthenticatedRequest] {
  import AuthenticatedAction.AuthenticatedRequest

  def transform[A](request: Request[A]): Future[AuthenticatedRequest[A]] =
    request.headers.get("Authorization") match {
      case None =>
        // TODO: return a Future[Result] (or whatever is correct)
        Failure(RuntimeException("Forbidden"))
        
      case Some(token) =>
        sessionStore.ask(ref => SessionStore.VerifyToken(token, ref)) {
          case Success(SessionStore.TokenPassed) =>
            val user = User("foo", "bar", "baz")
            val session = Session("foo", "bar")
            val device = Device("foo", None, "baz", ZonedDateTime.now)
            Success(AuthenticatedRequest(user, session, device, request))
            
          case Success(SessionStore.TokenFailed(error)) =>
            Failure(error)
          case Success(SessionStore.TokenOtherError(error)) =>
            Failure(error)
          case Failure(error) =>
            Failure(error)
          //
        }
    }
}
