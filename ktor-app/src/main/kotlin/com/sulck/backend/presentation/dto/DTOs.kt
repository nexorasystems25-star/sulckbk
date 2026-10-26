package com.sulck.backend.presentation.dto

import kotlinx.serialization.Serializable
import java.util.UUID

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
    val createdAt: java.time.Instant
)

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
    val startDate: java.time.Instant,
    val targetDate: java.time.Instant,
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
    val createdAt: java.time.Instant,
    val completedAt: java.time.Instant?
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
    val lastDepositAt: java.time.Instant?,
    val lastMissedAt: java.time.Instant?,
    val lockoutUntil: java.time.Instant?,
    val isLocked: Boolean
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null
)