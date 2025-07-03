package net.jcain.spelt.models.events

trait Event(identifier: String,
            stateKey: String,
            depth: Int):
  val eventType: String
  def label: String = s"Event:${this.getClass.getSimpleName}Event"

case class MRoomCreate(identifier: String,
                       stateKey: String,
                       depth: Int) extends Event(identifier, stateKey, depth):
  val eventType: String = "m.room.create"
