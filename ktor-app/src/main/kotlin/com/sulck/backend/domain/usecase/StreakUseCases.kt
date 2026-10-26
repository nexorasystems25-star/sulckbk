package com.sulck.backend.domain.usecase

import com.sulck.backend.domain.entity.Streak
import com.sulck.backend.domain.entity.StreakEvent
import com.sulck.backend.domain.entity.StreakEventType
import com.sulck.backend.domain.entity.StreakStatus
import com.sulck.backend.domain.repository.StreakRepository
import com.sulck.backend.domain.repository.TransactionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository
) {
    operator fun invoke(userId: UUID): Streak? = streakRepository.findByUserId(userId)
}

class UpdateStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val transactionRepository: TransactionRepository
) {
    data class Input(
        val userId: UUID,
        val depositAmount: Long? = null,
        val missedDeposit: Boolean = false
    )

    operator fun invoke(input: Input): Streak {
        var streak = streakRepository.findByUserId(input.userId)
            ?: Streak(userId = input.userId)

        val today = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
        val lastDepositDay = streak.lastDepositAt?.truncatedTo(java.time.temporal.ChronoUnit.DAYS)

        if (input.missedDeposit || (lastDepositDay != null && today > lastDepositDay.plusSeconds(86400))) {
            // Missed a day
            return handleMissedDeposit(streak)
        } else if (input.depositAmount != null && input.depositAmount! > 0) {
            // Made a deposit
            return handleDeposit(streak)
        }

        return streak
    }

    private fun handleMissedDeposit(streak: Streak): Streak {
        val newMissedDays = streak.missedDays + 1
        val newStatus = when {
            newMissedDays >= 3 -> StreakStatus.LOCKED
            newMissedDays >= 1 -> StreakStatus.AT_RISK
            else -> streak.status
        }

        val updatedStreak = streak.copy(
            missedDays = newMissedDays,
            status = newStatus,
            lastMissedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        streakRepository.upsert(updatedStreak)
        streakRepository.recordEvent(StreakEvent(
            userId = streak.userId,
            eventType = StreakEventType.DEPOSIT_MISSED,
            streakBefore = streak.currentStreak,
            streakAfter = updatedStreak.currentStreak
        ))

        return updatedStreak
    }

    private fun handleDeposit(streak: Streak): Streak {
        val newStreakDays = if (streak.lastDepositAt != null) {
            val daysSinceLast = java.time.Duration.between(streak.lastDepositAt!!, Instant.now()).toDays()
            if (daysSinceLast <= 1) streak.currentStreak + 1 else 1
        } else 1

        val newLongestStreak = maxOf(streak.longestStreak, newStreakDays)
        val newStatus = if (streak.status == StreakStatus.LOCKED) StreakStatus.ACTIVE else streak.status

        val updatedStreak = streak.copy(
            currentStreak = newStreakDays,
            longestStreak = newLongestStreak,
            missedDays = 0,
            status = newStatus,
            lastDepositAt = Instant.now(),
            lockoutUntil = null,
            updatedAt = Instant.now()
        )

        streakRepository.upsert(updatedStreak)
        streakRepository.recordEvent(StreakEvent(
            userId = streak.userId,
            eventType = StreakEventType.DEPOSIT_MADE,
            streakBefore = streak.currentStreak,
            streakAfter = newStreakDays
        ))

        return updatedStreak
    }
}