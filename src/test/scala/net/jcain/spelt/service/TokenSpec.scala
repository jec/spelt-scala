package net.jcain.spelt.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import net.jcain.spelt.models.Config
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should._

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

class TokenSpec extends AnyWordSpecLike with Matchers {
  "keyPair()" should {
    "return a tuple with the private and public keys" in {
      val (privateKey, publicKey) = Token.keyPair
      privateKey shouldBe a [RSAPrivateKey]
      publicKey shouldBe a [RSAPublicKey]
      println(privateKey.toString)
    }
  }

  "generateAndSign()" should {
    "return a JWT string" in {
      // Generate JWT.
      val uuid = java.util.UUID.randomUUID().toString
      val jwt = Token.generateAndSign(uuid)

      // Verify and decode JWT.
      val (privateKey, publicKey) = Token.keyPair
      val algorithm = Algorithm.RSA256(publicKey, privateKey)
      val verifier = JWT.require(algorithm).withIssuer(Config.jwtIssuer).build();
      val decodedJwt = verifier.verify(jwt)

      // Check payload.
      decodedJwt.getSubject should equal (uuid)
    }
  }
}
