package com.sulck.backend.domain.entity

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Streak(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val missedDays: Int = 0,
    val status: StreakStatus = StreakStatus.ACTIVE,
    val lastDepositAt: Instant? = null,
    val lastMissedAt: Instant? = null,
    val lockoutUntil: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isLocked: Boolean
        get() = status == StreakStatus.LOCKED || lockoutUntil != null && lockoutUntil! > Instant.now()

    val daysSinceLastDeposit: Long?
        get() = lastDepositAt?.let { java.time.Duration.between(it, Instant.now()).toDays() }
}

enum class StreakStatus {
    ACTIVE, PAUSED, AT_RISK, LOCKED
}

@Serializable
data class StreakEvent(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val eventType: StreakEventType,
    val streakBefore: Int,
    val streakAfter: Int,
    val metadata: String? = null, // JSON
    val createdAt: Instant = Instant.now()
)

enum class StreakEventType {
    DEPOSIT_MADE, DEPOSIT_MISSED, STREAK_PAUSED, STREAK_RESUMED, STREAK_LOCKED, STREAK_UNLOCKED, MANUAL_ADJUSTMENT
}