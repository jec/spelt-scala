package net.jcain.spelt.models

case class User(identifier: String,
                encryptedPassword: String,
                email: String)
