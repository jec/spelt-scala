package net.jcain.spelt.models.events

import wvlet.airframe.ulid.ULID

/**
  * Base trait for all event case classes
  */
trait Event(
  identifier: String,
  stateKey: String,
  depth: Int
):
  /**
    * Matrix-specified event type
    *
    * @example `m.room.create`
    *
    */
  val eventType: String

  /**
    * Returns the label string used on the database node
    *
    * This always includes:
    *   1. `Event`
    *   1. the specific case class name with `Event` appended
    *
    * @example `Event:MRoomCreateEvent`
    */
  def label: String = s"Event:${this.getClass.getSimpleName}Event"

case class MRoomCreate(identifier: String = ULID.newULIDString,
                       stateKey: String = ULID.newULIDString,
                       depth: Int = 0) extends Event(identifier, stateKey, depth):
  val eventType: String = "m.room.create"

case class MRoomMember(identifier: String = ULID.newULIDString,
                       stateKey: String = ULID.newULIDString,
                       depth: Int = 0) extends Event(identifier, stateKey, depth):
  val eventType: String = "m.room.member"

case class MRoomPowerLevels(identifier: String = ULID.newULIDString,
                            stateKey: String = ULID.newULIDString,
                            depth: Int = 0) extends Event(identifier, stateKey, depth):
  val eventType: String = "m.room.power_levels"
