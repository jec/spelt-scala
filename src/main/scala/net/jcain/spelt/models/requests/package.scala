package net.jcain.spelt.models

import net.jcain.spelt.models.events.{CreateRoomEvent, PowerLevelEvent, StateEvent}

package object requests:
  case class CreateRoomRequest(creation_content: Option[CreateRoomEvent] = None,
                               initial_state: Option[Seq[StateEvent]] = None,
                               invite: Option[Seq[String]] = None,
                               invite_3pid: Option[Seq[Invite3Pid]] = None,
                               is_direct: Option[Boolean] = None,
                               name: Option[String] = None,
                               power_level_content_override: Option[Seq[PowerLevelEvent]] = None,
                               preset: Option[String] = None,
                               room_alias_name: Option[String] = None,
                               room_version: Option[String] = None,
                               topic: Option[String] = None,
                               `type`: Option[String] = None,
                               visibility: Option[String] = None)

  case class Invite3Pid(address: String,
                        medium: String,
                        id_server: String,
                        id_access_token: String)
