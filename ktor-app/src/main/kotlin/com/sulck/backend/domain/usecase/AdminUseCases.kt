package com.sulck.backend.domain.usecase

import com.sulck.backend.domain.repository.GoalRepository
import com.sulck.backend.domain.repository.TransactionRepository
import com.sulck.backend.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

class AdminUseCases @Inject constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) {
    data class Stats(
        val totalUsers: Long,
        val activeGoals: Long,
        val totalDeposits: Long,
        val totalVolume: Long
    )

    operator fun invoke(): Stats {
        // TODO: Implement actual queries
        return Stats(
            totalUsers = 0,
            activeGoals = 0,
            totalDeposits = 0,
            totalVolume = 0L
        )
    }
}