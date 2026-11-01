package com.sulck.backend.domain.usecase

object AdminUseCases {
    lateinit var stats: AdminUseCases.Stats

    data class Stats(
        val totalUsers: Long,
        val activeGoals: Long,
        val totalDeposits: Long,
        val totalVolume: Long
    )
}