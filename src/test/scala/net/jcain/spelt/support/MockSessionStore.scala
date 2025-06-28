package net.jcain.spelt.support

import net.jcain.spelt.models.Session
import net.jcain.spelt.store.SessionStore
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MockSessionStore:
  /**
   * Behaves as though no Session node exists in the database
   *
   * For `DeleteSession`, responds with `SessionDeletionFailed`
   *
   * @return keep same behavior
   */
  def apply(): Behavior[SessionStore.Request] = Behaviors.receiveMessage:
    case SessionStore.DeleteSession(_, replyTo) =>
      replyTo ! SessionStore.SessionDeletionFailed("Session not found")
      Behaviors.same
    case SessionStore.DeleteAllSessions(_, replyTo) =>
      replyTo ! SessionStore.AllSessionsDeletionFailed("User not found")
      Behaviors.same

  /**
   * Behaves as though `existingSession` is a node in the database
   *
   * For `GetOrCreateSession` responds with `existingSession`.
   * For `DeleteSession`, responds with `SessionDeleted`.
   * For `DeleteAllSessions`, responds with `AllSessionsDeleted`.
   *
   * @param existingSession a `Session` node assumed to exist
   *
   * @return keep same behavior
   */
  def apply(existingSession: Session): Behavior[SessionStore.Request] = Behaviors.receiveMessage:
    case SessionStore.GetOrCreateSession(_, _, deviceIdOption, _, replyTo) =>
      replyTo ! SessionStore.SessionCreated(existingSession.ulid, existingSession.token, deviceIdOption.get)
      Behaviors.same
    case SessionStore.DeleteSession(_, replyTo) =>
      replyTo ! SessionStore.SessionDeleted
      Behaviors.same
    case SessionStore.DeleteAllSessions(_, replyTo) =>
      replyTo ! SessionStore.AllSessionsDeleted
      Behaviors.same
