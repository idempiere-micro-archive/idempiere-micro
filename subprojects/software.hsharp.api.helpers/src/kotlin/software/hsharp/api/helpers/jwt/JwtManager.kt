package software.hsharp.api.helpers.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class JwtManager private constructor() {

    private fun doCreateToken(subject: String, role: String, loginModel: String): String {
        val now = Instant.now()
        val expiryDate = Date.from(now.plus(TOKEN_VALIDITY))
        return Jwts.builder()
                .setSubject(subject)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_LOGINMODEL, loginModel)
                .setExpiration(expiryDate)
                .setIssuedAt(Date.from(now))
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY)
                .compact()
    }

    private fun doParseToken(compactToken: String): Jws<Claims> {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(compactToken)
    }

    companion object {

        val CLAIM_ROLE = "role"
        val CLAIM_LOGINMODEL = "loginmodel"

        private val SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256
        private val TOKEN_VALIDITY = Duration.ofHours(12L)
        private val SECRET_KEY = generateKey()

        private var DEFAULT_SECURE_RANDOM: SecureRandom? = null

        private var jwtManager: JwtManager? = null
        protected fun getInstance(): JwtManager {
            if (jwtManager == null) {
                jwtManager = JwtManager()
            }
            return jwtManager!!
        }

        private fun generateKey(): SecretKey {
            val bytes = ByteArray(32)
            if (DEFAULT_SECURE_RANDOM == null) {
                DEFAULT_SECURE_RANDOM = SecureRandom()
                DEFAULT_SECURE_RANDOM!!.nextBytes(ByteArray(64))
            }
            DEFAULT_SECURE_RANDOM!!.nextBytes(bytes)

            return SecretKeySpec(bytes, SIGNATURE_ALGORITHM.jcaName)
        }

        /**
         * Builds a JWT with the given subject and role and returns it as a JWS signed compact String.
         */
        fun createToken(subject: String, roleModel: String, loginModel: String): String {
            return getInstance().doCreateToken(subject, roleModel, loginModel)
        }

        /**
         * Parses the given JWS signed compact JWT, returning the claims.
         * If this method returns without throwing an exception, the token can be trusted.
         */
        fun parseToken(compactToken: String): Jws<Claims> {
            return getInstance().doParseToken(compactToken)
        }
    }
}