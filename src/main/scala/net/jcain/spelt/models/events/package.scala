package net.jcain.spelt.models

package object events:
  case class CreateRoomEvent(creator: Option[String],
                             `m.federate`: Option[Boolean],
                             predecessor: Option[RoomReference],
                             room_version: Option[String],
                             `type`: Option[String])

  case class PowerLevelEvent(ban: Option[Int],
                             events: Map[String, Int],
                             events_default: Option[Int],
                             invite: Option[Int],
                             kick: Option[Int],
                             notifications: Map[String, Int],
                             redact: Option[Int],
                             state_default: Option[Int],
                             users: Map[String, Int],
                             users_default: Option[Int])

  case class RoomReference(event_id: String,
                           room_id: String)

  case class StateEvent(content: String,
                        state_key: Option[String],
                        `type`: String)
