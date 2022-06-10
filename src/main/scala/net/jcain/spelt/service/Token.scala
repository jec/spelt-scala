package net.jcain.spelt.service

import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import net.jcain.spelt.models.Config

import java.nio.file.{Files, Path}
import java.security.interfaces.{RSAPrivateCrtKey, RSAPrivateKey, RSAPublicKey}
import java.security.KeyFactory
import java.security.spec.{PKCS8EncodedKeySpec, RSAPublicKeySpec}

object Token {
  val (privateKey, publicKey) = {
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

  val algorithm: Algorithm = Algorithm.RSA256(publicKey, privateKey)

  val verifier: JWTVerifier = JWT.require(algorithm)
    .withIssuer(Config.jwtIssuer)
    .build

  def generateAndSign(uuid: String): String = {
    val now = java.time.Instant.now

    JWT.create()
      .withIssuer(Config.jwtIssuer)
      .withSubject(uuid)
      .withIssuedAt(java.util.Date.from(now))
      .withExpiresAt(java.util.Date.from(now.plusSeconds(3600)))
      .sign(algorithm)
  }

  def verify(jwt: String): Either[Throwable, DecodedJWT] = try {
    Right(verifier.verify(jwt))
  } catch {
    case error: JWTVerificationException => Left(error)
  }
}
