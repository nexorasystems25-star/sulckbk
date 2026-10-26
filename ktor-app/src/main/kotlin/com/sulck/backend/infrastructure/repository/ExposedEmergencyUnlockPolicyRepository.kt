package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.EmergencyUnlockPolicy
import com.sulck.backend.domain.repository.EmergencyUnlockPolicyRepository
import com.sulck.backend.infrastructure.schema.EmergencyUnlockPolicies
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedEmergencyUnlockPolicyRepository : EmergencyUnlockPolicyRepository, KoinComponent {
    override fun findAll(): List<EmergencyUnlockPolicy> = transaction {
        EmergencyUnlockPolicies.selectAll().map { it.toDomain() }
    }

    override fun findById(id: UUID): EmergencyUnlockPolicy? = transaction {
        EmergencyUnlockPolicies.select { EmergencyUnlockPolicies.id.eq(id) }
            .firstOrNull()?.toDomain()
    }

    override fun findActive(): List<EmergencyUnlockPolicy> = transaction {
        EmergencyUnlockPolicies.select { EmergencyUnlockPolicies.isActive.eq(true) }
            .map { it.toDomain() }
    }

    override fun upsert(policy: EmergencyUnlockPolicy) = transaction {
        (EmergencyUnlockPolicies.select { EmergencyUnlockPolicies.id.eq(policy.id) }.firstOrNull()
            ?.apply {
                name = policy.name
                description = policy.description
                maxUnlocksPerMonth = policy.maxUnlocksPerMonth
                maxUnlockDurationHours = policy.maxUnlockDurationHours
                requiresApproval = policy.requiresApproval
                allowedFromLockLevels = policy.allowedFromLockLevels.joinToString(",")
                allowedToLockLevels = policy.allowedToLockLevels.joinToString(",")
                isActive = policy.isActive
                updatedAt = policy.updatedAt
            }
            ?: EmergencyUnlockPolicyEntity.fromDomain(policy)
        ).toDomain()
    }

    override fun delete(id: UUID) = transaction {
        EmergencyUnlockPolicies.select { EmergencyUnlockPolicies.id.eq(id) }
            .firstOrNull()?.delete()
    }
}

class EmergencyUnlockPolicyEntity(id: UUID) {
    var name by EmergencyUnlockPolicies.name
    var description by EmergencyUnlockPolicies.description
    var maxUnlocksPerMonth by EmergencyUnlockPolicies.maxUnlocksPerMonth
    var maxUnlockDurationHours by EmergencyUnlockPolicies.maxUnlockDurationHours
    var requiresApproval by EmergencyUnlockPolicies.requiresApproval
    var allowedFromLockLevels by EmergencyUnlockPolicies.allowedFromLockLevels
    var allowedToLockLevels by EmergencyUnlockPolicies.allowedToLockLevels
    var isActive by EmergencyUnlockPolicies.isActive
    var createdAt by EmergencyUnlockPolicies.createdAt
    var updatedAt by EmergencyUnlockPolicies.updatedAt

    fun toDomain(): EmergencyUnlockPolicy = EmergencyUnlockPolicy(
        id = id,
        name = name,
        description = description,
        maxUnlocksPerMonth = maxUnlocksPerMonth,
        maxUnlockDurationHours = maxUnlockDurationHours,
        requiresApproval = requiresApproval,
        allowedFromLockLevels = allowedFromLockLevels.split(","),
        allowedToLockLevels = allowedToLockLevels.split(","),
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(policy: EmergencyUnlockPolicy): EmergencyUnlockPolicyEntity {
            return EmergencyUnlockPolicyEntity(policy.id).apply {
                name = policy.name
                description = policy.description
                maxUnlocksPerMonth = policy.maxUnlocksPerMonth
                maxUnlockDurationHours = policy.maxUnlockDurationHours
                requiresApproval = policy.requiresApproval
                allowedFromLockLevels = policy.allowedFromLockLevels.joinToString(",")
                allowedToLockLevels = policy.allowedToLockLevels.joinToString(",")
                isActive = policy.isActive
                createdAt = policy.createdAt
                updatedAt = policy.updatedAt
            }
        }
    }
}
