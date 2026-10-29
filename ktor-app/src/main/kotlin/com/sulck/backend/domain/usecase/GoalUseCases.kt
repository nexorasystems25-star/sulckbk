package com.sulck.backend.domain.usecase

import com.sulck.backend.domain.entity.Goal
import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.entity.GoalTier
import com.sulck.backend.domain.repository.GoalRepository
import com.sulck.backend.domain.repository.UserRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository
) {
    data class Input(
        val userId: UUID,
        val title: String,
        val description: String,
        val targetAmount: Long,
        val currency: String = "GHS",
        val durationDays: Int,
        val cadenceDays: Int = 1,
        val lockedAssetSerial: String? = null
    )

    data class Output(
        val goal: Goal,
        val downPaymentAmount: Long,
        val dailyCommitment: Long
    )

    operator fun invoke(input: Input): Output {
        val user = userRepository.findById(input.userId)
            ?: throw IllegalArgumentException("User not found")

        val targetDate = Instant.now().plusSeconds(input.durationDays * 24L * 60L * 60L)
        val tier = GoalTier.fromDays(input.durationDays)
        val downPaymentPercent = tier.downPaymentPercent()
        val downPaymentAmount = (input.targetAmount * downPaymentPercent / 100)
        val dailyCommitment = (input.targetAmount - downPaymentAmount) / input.durationDays

        val goal = Goal(
            userId = input.userId,
            title = input.title,
            description = input.description,
            targetAmount = input.targetAmount,
            currency = input.currency,
            dailyCommitment = dailyCommitment,
            cadenceDays = input.cadenceDays,
            targetDate = targetDate,
            lockedAssetSerial = input.lockedAssetSerial
        )

        val createdGoal = goalRepository.create(goal)

        return Output(createdGoal, downPaymentAmount, dailyCommitment)
    }

    private fun GoalTier.fromDays(days: Int): GoalTier = when {
        days <= 90 -> GoalTier.SHORT_TERM
        days <= 365 -> GoalTier.MEDIUM_TERM
        else -> GoalTier.LONG_TERM
    }
}

class GetGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    operator fun invoke(id: UUID): Goal? = goalRepository.findById(id)
}

class ListGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    operator fun invoke(userId: UUID, activeOnly: Boolean = true): List<Goal> = if (activeOnly) {
        goalRepository.findActiveByUserId(userId)
    } else {
        goalRepository.findByUserId(userId)
    }
}