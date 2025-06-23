package net.jcain.spelt.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.{JWTDecodeException, SignatureVerificationException, TokenExpiredException}
import net.jcain.spelt.models.Config
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.*
import org.scalatest.wordspec.AnyWordSpecLike
import wvlet.airframe.ulid.ULID

class TokenSpec extends AnyWordSpecLike with Matchers {
  "generateAndSign()" should {
    "return a JWT string" in {
      // Generate JWT.
      val ulid = ULID.newULIDString
      val jwt = Token.generateAndSign(ulid)

      // Verify and decode JWT.
      val algorithm = Algorithm.RSA256(Token.publicKey, Token.privateKey)
      val verifier = JWT.require(algorithm).withIssuer(Config.jwtIssuer).build()
      val decodedJwt = verifier.verify(jwt)

      // Check payload.
      decodedJwt.getSubject should equal (ulid)
    }
  }

  "verify()" when {
    "token is a valid JWT" should {
      "return a decoded JWT" in {
        val ulid = ULID.newULIDString

        inside(Token.verify(Token.generateAndSign(ulid))) {
          case Right(decodedJwt) =>
            decodedJwt.getSubject should equal (ulid)
        }
      }
    }

    "token is not a JWT" should {
      "return a JWTDecodeException" in {
        inside(Token.verify("foo")) {
          case Left(error) =>
            error shouldBe a [JWTDecodeException]
        }
      }
    }

    "token signature is invalid" should {
      "return a SignatureVerificationException" in {
        val token = Token.generateAndSign("foo") + "x"

        inside(Token.verify(token)) {
          case Left(error) =>
            error shouldBe a [SignatureVerificationException]
        }
      }
    }

    "token has expired" should {
      "return a TokenExpiredException" in {
        val now = java.time.Instant.now

        val token = JWT.create()
          .withIssuer(Config.jwtIssuer)
          .withSubject("foo")
          .withIssuedAt(now.minusSeconds(3600))
          .withExpiresAt(now.minusSeconds(1800))
          .sign(Token.algorithm)

        inside(Token.verify(token)) {
          case Left(error) =>
            error shouldBe a [TokenExpiredException]
        }
      }
    }
  }
}
