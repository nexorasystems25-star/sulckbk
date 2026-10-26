package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.EmergencyUnlockRequest
import com.sulck.backend.domain.entity.UnlockRequestStatus
import com.sulck.backend.domain.repository.EmergencyUnlockRequestRepository
import com.sulck.backend.infrastructure.schema.EmergencyUnlockRequests
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedEmergencyUnlockRequestRepository : EmergencyUnlockRequestRepository, KoinComponent {
    override fun findByUserId(userId: UUID): List<EmergencyUnlockRequest> = transaction {
        EmergencyUnlockRequests.select { EmergencyUnlockRequests.userId.eq(userId) }
            .map { it.toDomain() }
    }

    override fun findById(id: UUID): EmergencyUnlockRequest? = transaction {
        EmergencyUnlockRequests.select { EmergencyUnlockRequests.id.eq(id) }
            .firstOrNull()?.toDomain()
    }

    override fun findAllPending(): List<EmergencyUnlockRequest> = transaction {
        EmergencyUnlockRequests.select { EmergencyUnlockRequests.status.eq("PENDING") }
            .map { it.toDomain() }
    }

    override fun upsert(request: EmergencyUnlockRequest) = transaction {
        (EmergencyUnlockRequests.select { EmergencyUnlockRequests.id.eq(request.id) }.firstOrNull()
            ?.apply {
                userId = request.userId
                contactId = request.contactId
                policyId = request.policyId
                requestedBy = request.requestedBy
                fromLockLevel = request.fromLockLevel
                toLockLevel = request.toLockLevel
                reason = request.reason
                durationHours = request.durationHours
                status = request.status.name
                approvedBy = request.approvedBy
                approvedAt = request.approvedAt
                rejectedBy = request.rejectedBy
                rejectedAt = request.rejectedAt
                rejectionReason = request.rejectionReason
                effectiveAt = request.effectiveAt
                expiresAt = request.expiresAt
                updatedAt = request.updatedAt
            }
            ?: EmergencyUnlockRequestEntity.fromDomain(request)
        ).toDomain()
    }

    override fun updateStatus(id: UUID, status: UnlockRequestStatus) = transaction {
        EmergencyUnlockRequests.select { EmergencyUnlockRequests.id.eq(id) }
            .firstOrNull()?.apply {
                this.status = status.name
                updatedAt = java.time.Instant.now()
            }
    }

    override fun delete(id: UUID) = transaction {
        EmergencyUnlockRequests.select { EmergencyUnlockRequests.id.eq(id) }
            .firstOrNull()?.delete()
    }
}

class EmergencyUnlockRequestEntity(id: UUID) {
    var userId by EmergencyUnlockRequests.userId
    var contactId by EmergencyUnlockRequests.contactId
    var policyId by EmergencyUnlockRequests.policyId
    var requestedBy by EmergencyUnlockRequests.requestedBy
    var fromLockLevel by EmergencyUnlockRequests.fromLockLevel
    var toLockLevel by EmergencyUnlockRequests.toLockLevel
    var reason by EmergencyUnlockRequests.reason
    var durationHours by EmergencyUnlockRequests.durationHours
    var status by EmergencyUnlockRequests.status
    var approvedBy by EmergencyUnlockRequests.approvedBy
    var approvedAt by EmergencyUnlockRequests.approvedAt
    var rejectedBy by EmergencyUnlockRequests.rejectedBy
    var rejectedAt by EmergencyUnlockRequests.rejectedAt
    var rejectionReason by EmergencyUnlockRequests.rejectionReason
    var effectiveAt by EmergencyUnlockRequests.effectiveAt
    var expiresAt by EmergencyUnlockRequests.expiresAt
    var createdAt by EmergencyUnlockRequests.createdAt
    var updatedAt by EmergencyUnlockRequests.updatedAt

    fun toDomain(): EmergencyUnlockRequest = EmergencyUnlockRequest(
        id = id,
        userId = userId,
        contactId = contactId,
        policyId = policyId,
        requestedBy = requestedBy,
        fromLockLevel = fromLockLevel,
        toLockLevel = toLockLevel,
        reason = reason,
        durationHours = durationHours,
        status = UnlockRequestStatus.valueOf(status),
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        rejectedBy = rejectedBy,
        rejectedAt = rejectedAt,
        rejectionReason = rejectionReason,
        effectiveAt = effectiveAt,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(request: EmergencyUnlockRequest): EmergencyUnlockRequestEntity {
            return EmergencyUnlockRequestEntity(request.id).apply {
                userId = request.userId
                contactId = request.contactId
                policyId = request.policyId
                requestedBy = request.requestedBy
                fromLockLevel = request.fromLockLevel
                toLockLevel = request.toLockLevel
                reason = request.reason
                durationHours = request.durationHours
                status = request.status.name
                approvedBy = request.approvedBy
                approvedAt = request.approvedAt
                rejectedBy = request.rejectedBy
                rejectedAt = request.rejectedAt
                rejectionReason = request.rejectionReason
                effectiveAt = request.effectiveAt
                expiresAt = request.expiresAt
                createdAt = request.createdAt
                updatedAt = request.updatedAt
            }
        }
    }
}
