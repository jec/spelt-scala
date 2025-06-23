package net.jcain.spelt.controllers

import net.jcain.spelt.models.Config
import net.jcain.spelt.service.Auth
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Scheduler}
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.*

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.util.{Failure, Success}

@Singleton
class LoginController @Inject() (
  @Named("AuthActor") authRef: ActorRef[Auth.Request],
  val controllerComponents: ControllerComponents
)(
  implicit xc: ExecutionContext,
  sch: Scheduler
) extends BaseController {
  /**
   * GET /_matrix/client/v3/login
   *
   * Returns the supported login flows
   *
   * Currently, the only supported login flow is "m.login.password".
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#get_matrixclientv3login
   */
  def loginTypes(): Action[AnyContent] = Action {
    Ok(Json.obj("flows" -> Json.arr(Json.obj("type" -> "m.login.password"))))
  }

  /**
   * POST /_matrix/client/v3/login
   *
   * Authenticates a user and, if successful, responds with a token
   *
   * See https://spec.matrix.org/v1.14/client-server-api/#post_matrixclientv3login
   */
  def logIn(): Action[JsValue] = Action.async(parse.json) { request =>
    implicit val timeout: Timeout = 5.seconds
    val body = request.body

    authRef.ask(ref => Auth.LogIn(body, ref))
      .map {
        case Auth.LoginSucceeded(identifier, token, deviceId) =>
          Ok(Json.obj(
            "access_token" -> JsString(token),
            "device_id" -> JsString(deviceId),
            "user_id" -> JsString(identifier),
            "well_known" -> Config.wellKnown
          ))

        case Auth.LoginFailed(message) =>
          Unauthorized(Json.obj("error_message" -> JsString(message)))
      }
  }
}
