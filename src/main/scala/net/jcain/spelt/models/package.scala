package net.jcain.spelt

package object models:
  case class Event(`type`: String,
                   stateKey: String,
                   content: String,
                   depth: Int)

  case class Room(identifier: String,
                  name: Option[String],
                  topic: Option[String],
                  avatar: Option[String],
                  alias: Option[String],
                  roomVersion: String)
