package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.User
import com.sulck.backend.domain.repository.UserRepository
import com.sulck.backend.infrastructure.repository.Entities.UserEntity
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import org.jetbrains.koin.core.inject
import java.util.UUID

class ExposedUserRepository : UserRepository, KoinComponent {
    override suspend fun create(user: User): User = transaction {
        val entity = UserEntity.new {
            id = user.id
            email = user.email
            phone = user.phone
            fullName = user.fullName
            passwordHash = user.passwordHash
            isActive = user.isActive
            isVerified = user.isVerified
            createdAt = user.createdAt
            updatedAt = user.updatedAt
            lastLoginAt = user.lastLoginAt
        }
        entity.toDomain()
    }

    override suspend fun findById(id: UUID): User? = transaction {
        UserEntity.findById(id)?.toDomain()
    }

    override suspend fun findByEmail(email: String): User? = transaction {
        (Users.select { Users.email.eq(email) }.firstOrNull())?.toDomain()
    }

    override suspend fun findByPhone(phone: String): User? = transaction {
        (Users.select { Users.phone.eq(phone) }.firstOrNull())?.toDomain()
    }

    override suspend fun update(user: User): User = transaction {
        val entity = UserEntity.findById(id) ?: return user
        entity.email = user.email
        entity.phone = user.phone
        entity.fullName = user.fullName
        entity.passwordHash = user.passwordHash
        entity.isActive = user.isActive
        entity.isVerified = user.isVerified
        entity.updatedAt = user.updatedAt
        entity.lastLoginAt = user.lastLoginAt
        entity.toDomain()
    }

    override suspend fun updateLastLogin(id: UUID, lastLoginAt: java.time.Instant): Boolean = transaction {
        val entity = UserEntity.findById(id) ?: return false
        entity.lastLoginAt = lastLoginAt
        entity.updatedAt = java.time.Instant.now()
        true
    }

    override suspend fun setVerified(id: UUID, verified: Boolean): Boolean = transaction {
        val entity = UserEntity.findById(id) ?: return false
        entity.isVerified = verified
        entity.updatedAt = java.time.Instant.now()
        true
    }

    override suspend fun setActive(id: UUID, active: Boolean): Boolean = transaction {
        val entity = UserEntity.findById(id) ?: return false
        entity.isActive = active
        entity.updatedAt = java.time.Instant.now()
        true
    }
}