package com.sulck.backend.presentation.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID
import java.time.Instant

// UUID and Instant serializers using kotlinx-serialization built-in
val UUID_serializer: KSerializer<UUID> = kotlinx.serialization.serializers.UUIDSerializer
val Instant_serializer: KSerializer<Instant> = kotlinx.serialization.serializers.InstantSerializer

// Helper for serializing Lists of serializable types
fun <T : Serializable> listSerializer(of: KSerializer<T>): KSerializer<List<T>> {
    return kotlinx.serialization.serializers.ListSerializer(of)
}

@Serializable
sealed class AuthRequest {
    @Serializable
    data class Register(
        val email: String,
        val phone: String,
        val fullName: String,
        val password: String
    ) : AuthRequest()

    @Serializable
    data class Login(
        val emailOrPhone: String,
        val password: String
    ) : AuthRequest()

    @Serializable
    data class Refresh(
        val refreshToken: String
    ) : AuthRequest()
}

@Serializable
sealed class AuthResponse {
    @Serializable
    data class Register(val user: UserSummary) : AuthResponse()

    @Serializable
    data class Login(
        val user: UserSummary,
        val accessToken: String,
        val refreshToken: String
    ) : AuthResponse()

    @Serializable
    data class Refresh(
        val accessToken: String,
        val refreshToken: String
    ) : AuthResponse()

    @Serializable
    data class Me(val user: UserSummary) : AuthResponse()
}

@Serializable
data class UserSummary(
    val id: UUID,
    val email: String,
    val phone: String,
    val fullName: String,
    val isActive: Boolean,
    val isVerified: Boolean,
    val createdAt: Instant
) : Serializable // explicit to ensure compatibility

@Serializable
sealed class GoalRequest {
    @Serializable
    data class Create(
        val title: String,
        val description: String,
        val targetAmount: Long,
        val currency: String? = null,
        val durationDays: Int,
        val cadenceDays: Int? = null,
        val lockedAssetSerial: String? = null
    ) : GoalRequest()
}

@Serializable
sealed class GoalResponse {
    @Serializable
    data class Create(
        val goal: GoalSummary,
        val downPaymentAmount: Long,
        val dailyCommitment: Long
    ) : GoalResponse()

    @Serializable
    data class List(val goals: List<GoalSummary>) : GoalResponse()

    @Serializable
    data class Detail(val goal: GoalDetail) : GoalResponse()
}

@Serializable
data class GoalSummary(
    val id: UUID,
    val title: String,
    val targetAmount: Long,
    val currency: String,
    val currentProgress: Long,
    val dailyCommitment: Long,
    val cadenceDays: Int,
    val status: String,
    val progressPercent: Double,
    val daysRemaining: Long,
    val tier: String
)

@Serializable
data class GoalDetail(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val description: String,
    val targetAmount: Long,
    val currency: String,
    val dailyCommitment: Long,
    val cadenceDays: Int,
    val startDate: Instant,
    val targetDate: Instant,
    val lockedAssetSerial: String?,
    val status: String,
    val currentProgress: Long,
    val downPaymentPaid: Boolean,
    val progressPercent: Double,
    val daysRemaining: Long,
    val tier: String
)

@Serializable
sealed class TransactionRequest {
    @Serializable
    data class Deposit(
        val goalId: String,
        val amount: Long,
        val provider: String,
        val phoneNumber: String
    ) : TransactionRequest()
}

@Serializable
sealed class TransactionResponse {
    @Serializable
    data class Deposit(
        val transaction: TransactionSummary,
        val paymentUrl: String?,
        val reference: String
    ) : TransactionResponse()

    @Serializable
    data class List(val transactions: List<TransactionSummary>) : TransactionResponse()
}

@Serializable
data class TransactionSummary(
    val id: UUID,
    val userId: UUID,
    val goalId: UUID?,
    val amount: Long,
    val currency: String,
    val type: String,
    val status: String,
    val provider: String?,
    val providerRef: String?,
    val description: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)

@Serializable
data class StreakResponse(
    val streak: StreakSummary?
)

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
data class DeviceStatusResponse(
    val devices: List<DeviceStatus>
)

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
data class EmergencyUnlockRequestResponse(
    val request: EmergencyUnlockRequestSummary?
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
    val rejectionReason: String?,
    val effectiveAt: Instant?,
    val expiresAt: Instant?,
    val createdAt: Instant
)

@Serializable
data class EmergencyContactResponse(
    val contacts: List<EmergencyContactSummary>
)

@Serializable
data class EmergencyContactSummary(
    val id: UUID,
    val userId: UUID,
    val contactType: String,
    val contactValue: String,
    val status: String,
    val priority: String,
    val reason: String?,
    val notes: String?,
    val resolvedAt: Instant?,
    val createdAt: Instant
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null
)

@Serializable
data class EmergencyUnlockInput(
    val userId: UUID,
    val policyId: UUID,
    val fromLockLevel: String,
    val toLockLevel: String,
    val reason: String,
    val durationHours: Int? = null,
    val contactId: UUID? = null
)

@Serializable
data class RejectUnlockInput(
    val reason: String
)

// Paystack webhook DTOs
@Serializable
data class PaystackWebhookData(
    val event: String,
    val data: PaystackWebhookData.Data
) {
    @Serializable
    data class Data(
        val id: Long,
        val domain: String,
        val status: String,
        val reference: String,
        val amount: Long,
        val currency: String,
        val channel: String,
        val paid_at: String,
        val created_at: String,
        val customer: Customer,
        val authorization: Authorization
    ) {
        @Serializable
        data class Customer(
            val id: Long,
            val email: String,
            val phone: String?
        )
        @Serializable
        data class Authorization(
            val authorization_code: String,
            val card_type: String,
            val last4: String,
            val exp_month: String,
            val exp_year: String,
            val bin: String,
            val bank: String,
            val brand: String,
            val reusable: Boolean
        )
    }
}