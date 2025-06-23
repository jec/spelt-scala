package net.jcain.spelt.models

case class User(name: String,
                encryptedPassword: String,
                email: String)
