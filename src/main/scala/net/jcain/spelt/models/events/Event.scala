package net.jcain.spelt.models.events

import wvlet.airframe.ulid.ULID

trait Event(identifier: String,
            stateKey: String,
            depth: Int):
  val eventType: String
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
