package com.sulck.backend.presentation.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID
import java.time.Instant

// UUID and Instant serializers using kotlinx-serialization built-in
val UUID_serializer: KSerializer<UUID> = kotlinx.serialization.serializers.builtInSerializers().serializer(java.type.UUID::class.java)
val Instant_serializer: KSerializer<Instant> = kotlinx.serialization.serializers.builtInSerializers().serializer(java.time.Instant::class.java)

// Helper for serializing Lists of serializable types
fun <T : Serializable> listSerializer(of: KSerializer<T>): KSerializer<List<T>> {
    return kotlinx.serialization.serializers.ListSerializer(of)
}

// ==================== Transaction DTOs ====================

@Serializable
data class TransactionResponse {
    data class Deposit(
        val transaction: com.sulck.backend.domain.entity.Transaction,
        val paymentUrl: String?,
        val reference: String
    )

    data class List(
        val transactions: List<com.sulck.backend.domain.entity.Transaction>
    )

    data class Error(
        val message: String
    )
}

// ==================== Error DTO ====================

@Serializable
data class ErrorResponse(
    val message: String
)

// ==================== Goal DTOs ====================

@Serializable
data class GoalResponse {
    data class Create(
        val goal: com.sulck.backend.domain.entity.Goal,
        val downPaymentAmount: Long,
        val dailyCommitment: Long
    )

    data class List(
        val goals: List<com.sulck.backend.domain.entity.Goal>
    )

    data class Detail(
        val goal: com.sulck.backend.domain.entity.Goal
    )
}

// ==================== Streak DTOs ====================

@Serializable
data class StreakSummary(
    val id: UUID,
    val userId: UUID,
    val currentStreak: Int,
    val longestStreak: Int,
    val missedDays: Int,
    val status: String,
    val lockLevel: String,
    val lastDepositAt: Instant?,
    val lastMissedAt: Instant?,
    val lockoutUntil: Instant?,
    val tempLockTimestamp: Instant?,
    val appLockTimestamp: Instant?,
    val warningIssuedAt: Instant?,
    val isLocked: Boolean,
    val isAppLocked: Boolean,
    val isTempLocked: Boolean,
    val isFullyLocked: Boolean
)

@Serializable
data class StreakResponse(
    val summary: StreakSummary
)

// ==================== Authentication DTOs ====================

@Serializable
data class AuthResponse {
    data class Register(
        val user: com.sulck.backend.domain.entity.User
    )

    data class Login(
        val result: String
    )

    data class Refresh(
        val result: String
    )

    data class Me(
        val user: com.sulck.backend.domain.entity.User
    )
}

// ==================== Emergency Unlock DTOs ====================

@Serializable
data class EmergencyUnlockInput(
    val userId: UUID,
    val policyId: UUID,
    val requestedBy: UUID,
    val fromLockLevel: String,
    val toLockLevel: String,
    val reason: String,
    val durationHours: Int?,
    val contactId: UUID?
)

@Serializable
data class RejectUnlockInput(
    val reason: String
)

@Serializable
data class EmergencyUnlockRequestSummary(
    val id: UUID,
    val userId: UUID,
    val policyId: UUID,
    val requestedBy: UUID,
    val fromLockLevel: String,
    val toLockLevel: String,
    val reason: String,
    val durationHours: Int?,
    val status: String,
    val approvedBy: UUID?,
    val approvedAt: Instant?,
    val rejectedBy: UUID?,
    val rejectedAt: Instant?,
    val effectiveAt: Instant?,
    val expiresAt: Instant?,
    val createdAt: Instant
)

@Serializable
data class EmergencyUnlockRequestResponse(
    val summary: EmergencyUnlockRequestSummary
)

// ==================== Device Status DTOs ====================

// DeviceStatus is defined as a nested class in EmergencyUnlockUseCases.GetAllDevicesStatusUseCase.
// Here we extract it as a top-level presentation DTO with matching properties.
@Serializable
data class DeviceStatus(
    val userId: UUID,
    val userEmail: String,
    val userPhone: String,
    val lockLevel: String,
    val missedDays: Int,
    val currentStreak: Int,
    val status: String,
    val isLocked: Boolean,
    val isAppLocked: Boolean,
    val isTempLocked: Boolean,
    val isFullyLocked: Boolean,
    val lastDepositAt: Instant?,
    val lastMissedAt: Instant?,
    val lockoutUntil: Instant?,
    val tempLockTimestamp: Instant?,
    val appLockTimestamp: Instant?,
    val lockedAssetSerial: String?,
    val goalProgress: Double?,
    val goalTarget: Long?,
    val goalRemaining: Long?
)

@Serializable
data class DeviceStatusResponse(
    val deviceStatusList: List<DeviceStatus>
)

// ==================== Paystack DTOs ====================

@Serializable
data class PaystackWebhookData(
    val data: PaystackWebhookData.Data

    @Serializable
    data class Data(
        val reference: String,
        val amount: Long,
        val channel: String
    )
)

// ==================== Goal DTOs (continued) ====================

@Serializable
data class GoalRequest {
    data class Create(
        val title: String,
        val description: String,
        val targetAmount: Long,
        val currency: String = "GHS",
        val durationDays: Int,
        val cadenceDays: Int = 1,
        val lockedAssetSerial: String? = null
    )
}