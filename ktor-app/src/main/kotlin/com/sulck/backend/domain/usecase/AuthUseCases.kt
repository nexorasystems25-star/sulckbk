package com.sulck.backend.domain.usecase

import com.sulck.backend.config.JwtConfig
import com.sulck.backend.domain.entity.User
import com.sulck.backend.domain.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import at.favre.lib.crypto.bcrypt.BCrypt
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(
        email: String,
        phone: String,
        fullName: String,
        password: String
    ): User {
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        return userRepository.create(
            User(
                email = email,
                phone = phone,
                fullName = fullName,
                passwordHash = passwordHash
            )
        )
    }
}

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(id: UUID): User? = userRepository.findById(id)
}

class AuthenticateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)

    data class AuthResult(
        val user: User,
        val accessToken: String,
        val refreshToken: String
    )

    operator fun invoke(emailOrPhone: String, password: String): AuthResult? {
        val user = userRepository.findByEmail(emailOrPhone)
            ?: userRepository.findByPhone(emailOrPhone)
            ?: return null

        if (!user.isActive || !user.isVerified) return null
        if (!verifyPassword(password, user.passwordHash)) return null

        userRepository.updateLastLogin(user.id, Instant.now())

        val accessToken = createAccessToken(user.id)
        val refreshToken = createRefreshToken(user.id)

        return AuthResult(user, accessToken, refreshToken)
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.verifyer().verify(password.toCharArray(), hash).verified
        } catch (e: Exception) {
            false
        }
    }

    private fun createAccessToken(userId: UUID): String = JWT.create()
        .withSubject(userId.toString())
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withExpiresAt(Instant.now().plusSeconds(jwtConfig.accessTokenExpiryMinutes * 60L))
        .sign(algorithm)

    private fun createRefreshToken(userId: UUID): String = JWT.create()
        .withSubject(userId.toString())
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withExpiresAt(Instant.now().plusSeconds(jwtConfig.refreshTokenExpiryDays * 24L * 60L * 60L))
        .sign(algorithm)
}

class RefreshTokenUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)

    data class RefreshResult(
        val accessToken: String,
        val refreshToken: String
    )

    operator fun invoke(refreshToken: String): RefreshResult? {
        try {
            val verifier = jwtConfig.verifier
            val decoded = verifier.verify(refreshToken)
            val userId = UUID.fromString(decoded.subject)
            val user = userRepository.findById(userId) ?: return null
            if (!user.isActive) return null

            val newAccessToken = JWT.create()
                .withSubject(userId.toString())
                .withIssuer(jwtConfig.issuer)
                .withAudience(jwtConfig.audience)
                .withExpiresAt(Instant.now().plusSeconds(jwtConfig.accessTokenExpiryMinutes * 60L))
                .sign(algorithm)

            val newRefreshToken = JWT.create()
                .withSubject(userId.toString())
                .withIssuer(jwtConfig.issuer)
                .withAudience(jwtConfig.audience)
                .withExpiresAt(Instant.now().plusSeconds(jwtConfig.refreshTokenExpiryDays * 24L * 60L * 60L))
                .sign(algorithm)

            return RefreshResult(newAccessToken, newRefreshToken)
        } catch (e: Exception) {
            return null
        }
    }
}