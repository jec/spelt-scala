package net.jcain.spelt.json.Reads

import net.jcain.spelt.json.Reads.events.*
import net.jcain.spelt.models.events.{CreateRoomEvent, PowerLevelEvent, StateEvent}
import net.jcain.spelt.models.requests.{CreateRoomRequest, Invite3Pid}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads}

package object requests:
  implicit val createRoomRequest: Reads[CreateRoomRequest] = (
    (JsPath \ "creation_content").readNullable[CreateRoomEvent] and
      (JsPath \ "initial_state").readNullable[Seq[StateEvent]] and
      (JsPath \ "invite").readNullable[Seq[String]] and
      (JsPath \ "invite_3pid").readNullable[Seq[Invite3Pid]] and
      (JsPath \ "is_direct").readNullable[Boolean] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "power_lever_content_override").readNullable[Seq[PowerLevelEvent]] and
      (JsPath \ "preset").readNullable[String] and
      (JsPath \ "room_alias_name").readNullable[String] and
      (JsPath \ "room_version").readNullable[String] and
      (JsPath \ "topic").readNullable[String] and
      (JsPath \ "type").readNullable[String] and
      (JsPath \ "visibility").readNullable[String]
    )(CreateRoomRequest.apply _)

implicit val invite3Pid: Reads[Invite3Pid] = (
  (JsPath \ "address").read[String] and
    (JsPath \ "medium").read[String] and
    (JsPath \ "id_server").read[String] and
    (JsPath \ "id_access_token").read[String]
  )(Invite3Pid.apply _)
