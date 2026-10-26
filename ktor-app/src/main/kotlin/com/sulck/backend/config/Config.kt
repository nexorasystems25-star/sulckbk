package com.sulck.backend.config

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.generic.derivation.default.*

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val poolSize: Int = 20,
    val flywayLocations: List<String> = listOf("classpath:db/migration")
) {
    companion object {
        fun load(): DatabaseConfig = ConfigSource.fromConfig(ConfigFactory.load().getConfig("database")).loadOrThrow()
    }
}

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080
) {
    companion object {
        fun load(): ServerConfig = ConfigSource.fromConfig(ConfigFactory.load().getConfig("server")).loadOrThrow()
    }
}

data class JwtConfig(
    val secret: String,
    val issuer: String = "sulck.app",
    val audience: String = "sulck.app",
    val accessTokenExpiryMinutes: Int = 15,
    val refreshTokenExpiryDays: Int = 30
) {
    val verifier: com.auth0.jwt.JWTVerifier
        get() = com.auth0.jwt.JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()

    companion object {
        fun load(): JwtConfig = ConfigSource.fromConfig(ConfigFactory.load().getConfig("jwt")).loadOrThrow()
    }
}

data class PaystackConfig(
    val secretKey: String,
    val publicKey: String,
    val webhookSecret: String,
    val baseUrl: String = "https://api.paystack.co"
) {
    companion object {
        fun load(): PaystackConfig = ConfigSource.fromConfig(ConfigFactory.load().getConfig("paystack")).loadOrThrow()
    }
}

data class AppConfig(
    val database: DatabaseConfig = DatabaseConfig.load(),
    val server: ServerConfig = ServerConfig.load(),
    val jwt: JwtConfig = JwtConfig.load(),
    val paystack: PaystackConfig = PaystackConfig.load()
)