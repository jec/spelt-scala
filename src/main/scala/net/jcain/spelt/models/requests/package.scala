package net.jcain.spelt.models

import net.jcain.spelt.models.events.{CreateRoomEvent, PowerLevelEvent, StateEvent}

package object requests:
  case class CreateRoomRequest(creation_content: Option[CreateRoomEvent],
                               initial_state: Seq[StateEvent],
                               invite: Seq[String],
                               invite_3pid: Seq[Invite3Pid],
                               is_direct: Option[Boolean],
                               name: Option[String],
                               power_level_content_override: Seq[PowerLevelEvent],
                               preset: Option[String],
                               room_alias_name: Option[String],
                               room_version: Option[String],
                               topic: Option[String],
                               `type`: Option[String],
                               visibility: Option[String])

  case class Invite3Pid(address: String,
                        medium: String,
                        id_server: String,
                        id_access_token: String)
