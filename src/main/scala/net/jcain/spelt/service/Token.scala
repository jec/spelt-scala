package net.jcain.spelt.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import net.jcain.spelt.models.Config

import java.nio.file.{Files, Path}
import java.security.interfaces.{RSAPrivateCrtKey, RSAPrivateKey, RSAPublicKey}
import java.security.KeyFactory
import java.security.spec.{PKCS8EncodedKeySpec, RSAPublicKeySpec}

object Token {
  def keyPair: (RSAPrivateKey, RSAPublicKey) = {
    // Load private key.
    val file = getClass.getResource("/pkey.pk8")
    val keyBytes = Files.readAllBytes(Path.of(file.toURI))
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = new PKCS8EncodedKeySpec(keyBytes)
    val privateKey = keyFactory.generatePrivate(keySpec)

    // Calculate public key.
    val crtKey = privateKey.asInstanceOf[RSAPrivateCrtKey]
    val publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus, crtKey.getPublicExponent)
    val publicKey = keyFactory.generatePublic(publicKeySpec)

    (privateKey.asInstanceOf[RSAPrivateKey], publicKey.asInstanceOf[RSAPublicKey])
  }

  def generateAndSign(uuid: String): String = {
    val (privateKey, publicKey) = keyPair
    val algorithm = Algorithm.RSA256(publicKey, privateKey)
    val now = java.time.Instant.now
    JWT.create()
      .withIssuer(Config.jwtIssuer)
      .withSubject(uuid)
      .withIssuedAt(java.util.Date.from(now))
      .withExpiresAt(java.util.Date.from(now.plusSeconds(3600)))
      .sign(algorithm)
  }
}
