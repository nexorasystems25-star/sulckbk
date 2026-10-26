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
    val lockLevel: LockLevel = LockLevel.NONE,
    val lastDepositAt: Instant? = null,
    val lastMissedAt: Instant? = null,
    val lockoutUntil: Instant? = null,
    val tempLockTimestamp: Instant? = null, // M-KOPA-style temp lock timestamp
    val warningIssuedAt: Instant? = null, // When AT_RISK warning was issued
    val appLockTimestamp: Instant? = null, // Day 1 app lock timestamp
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isLocked: Boolean
        get() = lockLevel != LockLevel.NONE || status == StreakStatus.LOCKED || lockoutUntil != null && lockoutUntil! > Instant.now()

    val isTempLocked: Boolean
        get() = lockLevel == LockLevel.TEMP_LOCK || (tempLockTimestamp != null && tempLockTimestamp!! > Instant.now())

    val isAppLocked: Boolean
        get() = lockLevel == LockLevel.APP_LOCK || (appLockTimestamp != null && appLockTimestamp!! > Instant.now())

    val isFullyLocked: Boolean
        get() = lockLevel == LockLevel.FULL_LOCK || status == StreakStatus.LOCKED

    val daysSinceLastDeposit: Long?
        get() = lastDepositAt?.let { java.time.Duration.between(it, Instant.now()).toDays() }

    val consecutiveMissedDays: Int
        get() = if (lastDepositAt != null) {
            val daysSince = java.time.Duration.between(lastDepositAt!!, Instant.now()).toDays().toInt()
            if (daysSince > 0) daysSince else 0
        } else 0

    val shouldTriggerAppLock: Boolean
        get() = consecutiveMissedDays >= 1 && lockLevel == LockLevel.NONE

    val shouldTriggerTempLock: Boolean
        get() = consecutiveMissedDays >= 2 && lockLevel == LockLevel.APP_LOCK

    val shouldTriggerFullLock: Boolean
        get() = consecutiveMissedDays >= 3 && lockLevel != LockLevel.FULL_LOCK
}

enum class StreakStatus {
    ACTIVE, PAUSED, AT_RISK, LOCKED
}

enum class LockLevel {
    NONE,           // No lock
    APP_LOCK,       // Day 1: App-level screen lock with warning
    TEMP_LOCK,      // Day 2: Phone temp lock, app accessible with unlock button
    FULL_LOCK       // Day 3: Full phone lock
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