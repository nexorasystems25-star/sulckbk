package com.sulck.backend.domain.entity

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class EmergencyContact(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val contactType: ContactType,
    val contactValue: String, // Phone number, WhatsApp number, or chat session ID
    val status: ContactStatus = ContactStatus.OPEN,
    val priority: ContactPriority = ContactPriority.NORMAL,
    val reason: String? = null, // Reason for contact
    val notes: String? = null, // Admin notes
    val resolvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class ContactType {
    PHONE_CALL,
    WHATSAPP,
    IN_APP_CHAT,
    EMAIL
}

enum class ContactStatus {
    OPEN, IN_PROGRESS, RESOLVED, ESCALATED
}

enum class ContactPriority {
    LOW, NORMAL, HIGH, URGENT
}

@Serializable
data class EmergencyUnlockPolicy(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String,
    val maxUnlocksPerMonth: Int,
    val maxUnlockDurationHours: Int,
    val requiresApproval: Boolean = true,
    val allowedFromLockLevels: List<String>, // ["FULL_LOCK", "TEMP_LOCK", "APP_LOCK"]
    val allowedToLockLevels: List<String>, // ["TEMP_LOCK", "APP_LOCK", "NONE"]
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

@Serializable
data class EmergencyUnlockRequest(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val contactId: UUID?, // Link to emergency contact if applicable
    val policyId: UUID,
    val requestedBy: UUID, // Admin user ID
    val fromLockLevel: String,
    val toLockLevel: String,
    val reason: String,
    val durationHours: Int?,
    val status: UnlockRequestStatus = UnlockRequestStatus.PENDING,
    val approvedBy: UUID? = null,
    val approvedAt: Instant? = null,
    val rejectedBy: UUID? = null,
    val rejectedAt: Instant? = null,
    val rejectionReason: String? = null,
    val effectiveAt: Instant? = null,
    val expiresAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class UnlockRequestStatus {
    PENDING, APPROVED, REJECTED, APPLIED, EXPIRED
}
