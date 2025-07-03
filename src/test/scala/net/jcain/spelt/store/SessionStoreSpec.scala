package net.jcain.spelt.store

import net.jcain.spelt.models.User
import net.jcain.spelt.service.{Auth, Token}
import net.jcain.spelt.support.DatabaseRollback
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with DatabaseRollback {
  trait TargetActor {
    val store: ActorRef[SessionStore.Request] = testKit.spawn(SessionStore())
    val probe: TestProbe[SessionStore.Response] = testKit.createTestProbe[SessionStore.Response]()
  }

  trait ExistingUser extends TargetActor {
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
    val sessionStore: ActorRef[SessionStore.Request] = testKit.spawn(SessionStore())
    val sessionStoreProbe: TestProbe[SessionStore.Response] = testKit.createTestProbe[SessionStore.Response]()

    sessionStore !
      SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", None, None, sessionStoreProbe.ref)

    val existingSession: SessionStore.SessionCreated =
      sessionStoreProbe.expectMessageType[SessionStore.SessionCreated]
  }

  trait ExistingSessions extends ExistingSession {
    // Create a second session for the User.
    sessionStore !
      SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", None, None, sessionStoreProbe.ref)

    val existingSession2: SessionStore.SessionCreated =
      sessionStoreProbe.expectMessageType[SessionStore.SessionCreated]

    // Create a third session for the User.
    sessionStore !
      SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", None, None, sessionStoreProbe.ref)

    val existingSession3: SessionStore.SessionCreated =
      sessionStoreProbe.expectMessageType[SessionStore.SessionCreated]
  }

  "GetOrCreateSession" when {
    "User has no previous Session" should {
      "respond with SessionCreated" in new ExistingUser {
        store ! SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", None, None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            store ! SessionStore.VerifyToken(token, probe.ref)
            probe.expectMessageType[SessionStore.TokenPassed]
        }
      }
    }

    "User has a previous Session" should {
      "respond with SessionCreated with the same device ID and a new token" in new ExistingSession {
        sessionStore !
          SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", Some(existingSession.deviceId), None, sessionStoreProbe.ref)

        inside(sessionStoreProbe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            token should not equal existingSession.token
            deviceId should equal (existingSession.deviceId)
        }
      }
    }

    "previous Session not found" should {
      "respond with SessionCreated with the same device ID and a new token" in new ExistingUser {
        store ! SessionStore.GetOrCreateSession(existingUser.name, "1.2.3.4", Some("foo"), None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.SessionCreated(ulid, token, deviceId) =>
            ULID.fromString(ulid) shouldBe a [ULID]
            ULID.fromString(deviceId) shouldBe a [ULID]

            Token.verify(token) should matchPattern {
              case Right(_) =>
            }

            store ! SessionStore.VerifyToken(token, probe.ref)
            probe.expectMessageType[SessionStore.TokenPassed]
        }
      }
    }

    "User does not exist" should {
      "respond with UserNotFound" in new TargetActor {
        store ! SessionStore.GetOrCreateSession("phred", "1.2.3.4", None, None, probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.UserNotFound =>
            println("foo")
        }
      }
    }
  }

  "DeleteSession" when {
    "Session exists" should {
      "respond with SessionDeleted" in new ExistingSession {
        sessionStore ! SessionStore.DeleteSession(existingSession.ulid, sessionStoreProbe.ref)

        sessionStoreProbe.expectMessage(SessionStore.SessionDeleted)
      }
    }

    "Session does not exist" should {
      "respond with SessionDeletionFailed" in new TargetActor {
        store ! SessionStore.DeleteSession("foobar", probe.ref)

        inside(probe.expectMessageType[SessionStore.SessionDeletionFailed]) {
          case SessionStore.SessionDeletionFailed(error) =>
            error should include("ID foobar")
        }
      }
    }
  }

  "DeleteAllSessions" when {
    "User exists and has Sessions" should {
      "respond with AllSessionsDeleted" in new ExistingSessions {
        sessionStore ! SessionStore.DeleteAllSessions(existingUser.name, sessionStoreProbe.ref)

        sessionStoreProbe.expectMessage(SessionStore.AllSessionsDeleted)
      }
    }

    "User does not exist" should {
      "respond with SessionDeletionFailed" in new TargetActor {
        store ! SessionStore.DeleteAllSessions("foobar", probe.ref)

        inside(probe.expectMessageType[SessionStore.Response]) {
          case SessionStore.AllSessionsDeletionFailed(error) =>
            error should include("user foobar")
        }
      }
    }
  }

  "VerifyToken" when {
    "JWT is valid" when {
      "session exists" should {
        "respond with TokenPassed" in new ExistingSession {
          sessionStore ! SessionStore.VerifyToken(existingSession.token, sessionStoreProbe.ref)

          sessionStoreProbe.expectMessageType[SessionStore.TokenPassed]
        }
      }

      "session does not exist" should {
        "respond with TokenFailed" in new TargetActor {
          private val token = Token.generateAndSign(ULID.newULIDString)

          store ! SessionStore.VerifyToken(token, probe.ref)

          inside(probe.expectMessageType[SessionStore.Response]) {
            case SessionStore.TokenFailed(error) =>
              error.getMessage should include ("Session not found")
          }
        }
      }
    }
  }
}
