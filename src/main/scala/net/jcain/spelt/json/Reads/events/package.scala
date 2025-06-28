package net.jcain.spelt.json.Reads

import net.jcain.spelt.models.events.{CreateRoomEvent, PowerLevelEvent, RoomReference, StateEvent}
import net.jcain.spelt.service.Auth.{Identifier, PasswordLogin}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import play.api.libs.json.Reads.*

package object events:
  implicit val createRoomEvent: Reads[CreateRoomEvent] = (
    (JsPath \ "creator").readNullable[String] and
      (JsPath \ "m.federate").readNullable[Boolean] and
      (JsPath \ "predecessor").readNullable[RoomReference] and
      (JsPath \ "room_version").readNullable[String] and
      (JsPath \ "type").readNullable[String]
    )(CreateRoomEvent.apply _)
  
  implicit val powerLevelEvent: Reads[PowerLevelEvent] = (
    (JsPath \ "ban").readNullable[Int] and
      (JsPath \ "events").read[Map[String, Int]] and
      (JsPath \ "events_default").readNullable[Int] and
      (JsPath \ "invite").readNullable[Int] and
      (JsPath \ "kick").readNullable[Int] and
      (JsPath \ "notifications").read[Map[String, Int]] and
      (JsPath \ "redact").readNullable[Int] and
      (JsPath \ "state_default").readNullable[Int] and
      (JsPath \ "users").read[Map[String, Int]] and
      (JsPath \ "users_default").readNullable[Int]
    )(PowerLevelEvent.apply _)

  implicit val roomReference: Reads[RoomReference] = (
    (JsPath \ "event_id").read[String] and
      (JsPath \ "room_id").read[String]
    )(RoomReference.apply _)

  implicit val stateEvent: Reads[StateEvent] = (
    (JsPath \ "content").read[String] and
      (JsPath \ "state_key").readNullable[String] and
      (JsPath \ "type").read[String]
    )(StateEvent.apply _)
