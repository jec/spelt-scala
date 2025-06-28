package net.jcain.spelt.support

import net.jcain.spelt.models.User
import net.jcain.spelt.store.UserStore
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

/**
 * Implements the UserStore Request/Response flows without querying the database
 *
 * The caller determines the Response by using an appropriate version of `apply()`.
 */
object MockUserStore:
  /**
   * For `GetUser`, simulates no user found
   *
   * @return keep same behavior
   */
  def apply(): Behavior[UserStore.Request] = Behaviors.receiveMessage:
    case UserStore.GetUser(_name, replyTo) =>
      replyTo ! UserStore.GetUserResponse(Right(None))
      Behaviors.same

  /**
   * For `GetUser`, simulates finding the User node
   *
   * @param existingUser the `User` object with which to respond
   *
   * @return keep same behavior
   */
  def apply(existingUser: User): Behavior[UserStore.Request] = Behaviors.receiveMessage:
    case UserStore.GetUser(_, replyTo) =>
      replyTo ! UserStore.GetUserResponse(Right(Some(existingUser)))
      Behaviors.same
