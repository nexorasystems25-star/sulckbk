package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.Goal
import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.repository.GoalRepository
import com.sulck.backend.infrastructure.repository.Entities.GoalEntity
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedGoalRepository : GoalRepository, KoinComponent {
    override suspend fun create(goal: Goal): Goal = transaction {
        val entity = GoalEntity.fromDomain(goal)
        entity.toDomain()
    }

    override suspend fun findById(id: UUID): Goal? = transaction {
        GoalEntity.findById(id)?.toDomain()
    }

    override suspend fun findByUserId(userId: UUID): List<Goal> = transaction {
        Goals.select { Goals.userId.eq(userId) }
            .orderByDescending(Goals.createdAt)
            .map { it.toDomain() }
    }

    override suspend fun findActiveByUserId(userId: UUID): List<Goal> = transaction {
        Goals.select {
            Goals.userId.eq(userId) and Goals.status.inList(GoalStatus.ACTIVE, GoalStatus.PAUSED)
        }
        .orderByDescending(Goals.createdAt)
        .map { it.toDomain() }
    }

    override suspend fun update(goal: Goal): Goal = transaction {
        val entity = GoalEntity.findById(id) ?: return goal
        entity.title = goal.title
        entity.description = goal.description
        entity.targetAmount = goal.targetAmount
        entity.currency = goal.currency
        entity.dailyCommitment = goal.dailyCommitment
        entity.cadenceDays = goal.cadenceDays
        entity.startDate = goal.startDate
        entity.targetDate = goal.targetDate
        entity.lockedAssetSerial = goal.lockedAssetSerial
        entity.status = goal.status
        entity.currentProgress = goal.currentProgress
        entity.downPaymentPaid = goal.downPaymentPaid
        entity.updatedAt = goal.updatedAt
        entity.toDomain()
    }

    override suspend fun updateProgress(id: UUID, newProgress: Long): Boolean = transaction {
        val entity = GoalEntity.findById(id) ?: return false
        entity.currentProgress = newProgress
        entity.updatedAt = java.time.Instant.now()
        true
    }

    override suspend fun updateStatus(id: UUID, status: GoalStatus): Boolean = transaction {
        val entity = GoalEntity.findById(id) ?: return false
        entity.status = status
        entity.updatedAt = java.time.Instant.now()
        true
    }

    override suspend fun delete(id: UUID): Boolean = transaction {
        GoalEntity.findById(id)?.delete()
        true
    }
}