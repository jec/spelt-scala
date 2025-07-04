package net.jcain.spelt.controllers

import net.jcain.spelt.models.{Config, User}
import net.jcain.spelt.service.Auth
import net.jcain.spelt.store.{SessionStore, UserStore}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.Inside.inside
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import play.api.mvc.Headers
import play.api.test.*
import play.api.test.Helpers.*
import play.api.test.CSRFTokenHelper._
import wvlet.airframe.ulid.ULID

class AuthControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with DatabaseRollback {
  trait ExistingUser extends ScalaTestWithActorTestKit {
    val existingPassword = "open-sesame"
    val existingUser: User = User("phredsmerd", Auth.hashPassword(existingPassword), "phredsmerd@example.com")

    // Create User in database.
    private val userStore = testKit.spawn(UserStore())
    private val userStoreProbe = testKit.createTestProbe[UserStore.Response]()

    userStore ! UserStore.CreateUser(
      existingUser.name,
      existingPassword,
      existingUser.email,
      userStoreProbe.ref
    )

    userStoreProbe.expectMessage(UserStore.CreateUserResponse(Right(existingUser.name)))
  }

  trait ExistingSession extends ExistingUser {
    private val sessionStoreRepo = testKit.spawn(SessionStore())
    private val sessionStoreProbe = testKit.createTestProbe[SessionStore.Response]()

    sessionStoreRepo !
      SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", None, None, sessionStoreProbe.ref)

    val existingSession: SessionStore.SessionCreated =
      sessionStoreProbe.expectMessageType[SessionStore.SessionCreated]

    val authHeader: Headers = Headers("Authorization" -> s"Bearer ${existingSession.token}")
  }

  trait LoginRequestParams extends ExistingUser {
    val requestDeviceId: String = ULID.newULIDString
    val requestDeviceName = "iDevice 123 Max Pro Extreme"

    val identifierJson: JsObject = Json.obj(
      "type" -> "m.id.user",
      "user" -> existingUser.name
    )

    val parsedParams: JsObject = Json.obj(
      "identifier" -> identifierJson,
      "password" -> existingPassword,
      "type" -> "m.login.password",
      "device_id" -> requestDeviceId,
      "initial_device_display_name" -> requestDeviceName
    )
  }

  case class WellKnown(`m.homeserver`: String, `m.identity_server`: String)
  case class LoginResponse(access_token: String,
                           device_id: String,
                           user_id: String,
                           well_known: WellKnown)

  implicit val wellKnownReads: Reads[WellKnown] = (
    (JsPath \ "m.homeserver" \ "base_url").read[String] and
      (JsPath \ "m.identity_server" \ "base_url").read[String]
  )(WellKnown.apply _)

  implicit val loginResponseReads: Reads[LoginResponse] = (
    (JsPath \ "access_token").read[String] and
      (JsPath \ "device_id").read[String] and
      (JsPath \ "user_id").read[String] and
      (JsPath \ "well_known").read[WellKnown]
  )(LoginResponse.apply _)

  "GET /_matrix/client/v3/login" should {
    "return the available login types" in {
      val Some(response) = route(app, FakeRequest(GET, "/_matrix/client/v3/login")): @unchecked

      status(response) mustBe OK
      contentAsString(response) must equal ("{\"flows\":[{\"type\":\"m.login.password\"}]}")
    }
  }

  "POST /_matrix/client/v3/login" when {
    "credentials are valid" should {
      "log in the user and return a 200 with a JWT" in new LoginRequestParams {
        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/login").withBody(parsedParams)): @unchecked

        status(response) mustBe OK

        inside(contentAsJson(response).validate[LoginResponse]) {
          case JsSuccess(LoginResponse(jwt, deviceId, userId, WellKnown(homeUrl, idUrl)), _) =>
            // TODO: Implement Device model so this passes.
            // deviceId must equal (requestDeviceId)
            userId must equal (s"@${existingUser.name}:${Config.homeserver}")
            homeUrl must equal (s"https://${Config.homeserver}")
            idUrl must equal (Config.identityUrl)
        }
      }
    }

    "credentials are invalid" should {
      "return a 401" in {
        val identifier = Json.obj(
          "type" -> "m.id.user",
          "user" -> "phredsmerd"
        )

        val payload = Json.obj(
          "identifier" -> identifier,
          "password" -> "wrong-password",
          "type" -> "m.login.password"
        )

        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/login").withBody(payload)): @unchecked

        status(response) mustBe UNAUTHORIZED
      }
    }
  }

  "POST /_matrix/client/v3/logout" when {
    "user is logged in and Authorization header provided" should {
      "respond with 200/Ok" in new ExistingSession {
        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout").withHeaders(("Authorization", s"Bearer ${existingSession.token}")).withJsonBody(Json.obj()).withCSRFToken): @unchecked
        status(response) mustBe OK

        // A second request should be unauthorized.
        val Some(response_2) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout").withHeaders(("Authorization", s"Bearer ${existingSession.token}")).withJsonBody(Json.obj()).withCSRFToken): @unchecked
        status(response_2) mustBe UNAUTHORIZED
      }
    }

    "no Authorization header" should {
      "respond with 401/Unauthorized" in {
        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout").withJsonBody(Json.obj())): @unchecked

        status(response) mustBe UNAUTHORIZED
      }
    }
  }

  "POST /_matrix/client/v3/logout/all" when {
    "User is logged in and Authorization header provided" should {
      "respond with 200/Ok" in new ExistingSession {
        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout/all").withHeaders(("Authorization", s"Bearer ${existingSession.token}")).withJsonBody(Json.obj()).withCSRFToken): @unchecked
        status(response) mustBe OK

        // A second request should be unauthorized.
        val Some(response_2) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout/all").withHeaders(("Authorization", s"Bearer ${existingSession.token}")).withJsonBody(Json.obj()).withCSRFToken): @unchecked
        status(response_2) mustBe UNAUTHORIZED
      }
    }

    "no Authorization header" should {
      "respond with 401/Unauthorized" in {
        val Some(response) = route(app, FakeRequest(POST, "/_matrix/client/v3/logout/all").withJsonBody(Json.obj())): @unchecked

        status(response) mustBe UNAUTHORIZED
      }
    }
  }
}
