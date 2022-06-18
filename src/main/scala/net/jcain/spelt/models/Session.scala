package net.jcain.spelt.models

case class Session(uuid: String,
                   token: String,
                   deviceId: String,
                   deviceName: Option[String])
