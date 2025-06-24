package net.jcain.spelt.service

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.{JWT, JWTVerifier}
import net.jcain.spelt.models.Config
import wvlet.airframe.ulid.ULID

import java.nio.file.{Files, Path}
import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateCrtKey, RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, RSAPublicKeySpec}

/**
 * Implements functions to create and validate JWTs
 */
object Token {
  /**
   * RSA key used for JWT signatures
   */
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

  /**
   * Algorithm used for JWT signatures
   */
  val algorithm: Algorithm = Algorithm.RSA256(publicKey, privateKey)

  /**
   * Instance of JWT verifier
   */
  val verifier: JWTVerifier = JWT.require(algorithm)
    .withIssuer(Config.jwtIssuer)
    .build

  /**
   * Generates a JWT for the Session `ulid`
   *
   * The payload includes a JTI claim (JWT ID), which is a ULID generated at the time of the call.
   * This ensures that multiple JWTs generated for the same Session ULID within the same epoch
   * timestamp are unique. The JTI is never referenced otherwise.
   *
   * @param ulid Session ULID
   *
   * @return JWT for authentication
   */
  def generateAndSign(ulid: String): String = {
    val now = java.time.Instant.now
    val jwtId = ULID.newULIDString

    JWT.create()
      .withIssuer(Config.jwtIssuer)
      .withSubject(ulid)
      .withJWTId(jwtId)
      .withIssuedAt(now)
      .withExpiresAt(now.plusSeconds(3600))
      .sign(algorithm)
  }

  /**
   * Decodes a JWT
   *
   * @param token JWT
   *
   * @return either the payload of the JWT or a Throwable
   */
  def verify(token: String): Either[Throwable, DecodedJWT] = try {
    Right(verifier.verify(token))
  } catch {
    case error: JWTVerificationException => Left(error)
  }
}
