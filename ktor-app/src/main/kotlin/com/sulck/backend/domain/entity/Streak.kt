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
    val tempLockTimestamp: Instant? = null, // M-KOPA-style temp lock timestamp
    val warningIssuedAt: Instant? = null, // When AT_RISK warning was issued
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isLocked: Boolean
        get() = status == StreakStatus.LOCKED || lockoutUntil != null && lockoutUntil! > Instant.now()

    val isTempLocked: Boolean
        get() = tempLockTimestamp != null && tempLockTimestamp!! > Instant.now()

    val daysSinceLastDeposit: Long?
        get() = lastDepositAt?.let { java.time.Duration.between(it, Instant.now()).toDays() }

    val consecutiveMissedDays: Int
        get() = if (lastDepositAt != null) {
            val daysSince = java.time.Duration.between(lastDepositAt!!, Instant.now()).toDays().toInt()
            if (daysSince > 0) daysSince else 0
        } else 0

    val shouldTriggerWarning: Boolean
        get() = consecutiveMissedDays >= 1 && warningIssuedAt == null

    val shouldTriggerTempLock: Boolean
        get() = consecutiveMissedDays >= 2 && tempLockTimestamp == null

    val shouldTriggerFullLock: Boolean
        get() = consecutiveMissedDays >= 3 && status != StreakStatus.LOCKED
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