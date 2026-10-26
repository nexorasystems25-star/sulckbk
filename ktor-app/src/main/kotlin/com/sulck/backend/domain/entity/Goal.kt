package com.sulck.backend.domain.entity

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Goal(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val title: String,
    val description: String,
    val targetAmount: Long, // in minor units (cents/pesewas)
    val currency: String = "GHS",
    val dailyCommitment: Long, // in minor units
    val cadenceDays: Int = 1, // 1 = daily, 7 = weekly
    val startDate: Instant = Instant.now(),
    val targetDate: Instant,
    val lockedAssetSerial: String? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val currentProgress: Long = 0,
    val downPaymentPaid: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val progressPercent: Double
        get() = if (targetAmount > 0) (currentProgress.toDouble() / targetAmount).coerceIn(0.0, 1.0) else 0.0

    val daysRemaining: Long
        get() = java.time.Duration.between(Instant.now(), targetDate).toDays().coerceAtLeast(0)

    val isCompleted: Boolean
        get() = currentProgress >= targetAmount

    val tier: GoalTier
        get() = when {
            daysRemaining <= 90 -> GoalTier.SHORT_TERM
            daysRemaining <= 365 -> GoalTier.MEDIUM_TERM
            else -> GoalTier.LONG_TERM
        }
}

enum class GoalStatus {
    ACTIVE, PAUSED, COMPLETED, CANCELLED, LOCKED
}

enum class GoalTier {
    SHORT_TERM, // 0-90 days: 15% down payment
    MEDIUM_TERM, // 91-365 days: 10% down payment
    LONG_TERM // 366+ days: 5% down payment
}

fun GoalTier.downPaymentPercent(): Int = when (this) {
    GoalTier.SHORT_TERM -> 15
    GoalTier.MEDIUM_TERM -> 10
    GoalTier.LONG_TERM -> 5
}