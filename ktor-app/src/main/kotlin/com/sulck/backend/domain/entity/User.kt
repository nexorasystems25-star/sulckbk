package com.sulck.backend.domain.entity

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class User(
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val phone: String,
    val fullName: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastLoginAt: Instant? = null
) {
    val displayName: String
        get() = fullName.split(" ").first()
}