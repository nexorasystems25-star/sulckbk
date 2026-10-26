package com.sulck.backend.domain.repository

import com.sulck.backend.domain.entity.User
import java.util.UUID

interface UserRepository {
    suspend fun create(user: User): User
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByPhone(phone: String): User?
    suspend fun update(user: User): User
    suspend fun updateLastLogin(id: UUID, lastLoginAt: java.time.Instant): Boolean
    suspend fun setVerified(id: UUID, verified: Boolean): Boolean
    suspend fun setActive(id: UUID, active: Boolean): Boolean
}