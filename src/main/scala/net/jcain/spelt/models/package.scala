package net.jcain.spelt

package object models:
  case class Room(identifier: String,
                  name: Option[String] = None,
                  topic: Option[String] = None,
                  avatar: Option[String] = None,
                  alias: Option[String] = None,
                  roomVersion: String)
