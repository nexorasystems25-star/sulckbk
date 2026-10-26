package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.Streak
import com.sulck.backend.domain.entity.StreakEvent
import com.sulck.backend.domain.entity.StreakEventType
import com.sulck.backend.domain.entity.StreakStatus
import com.sulck.backend.domain.repository.StreakRepository
import com.sulck.backend.infrastructure.repository.Entities.StreakEntity
import com.sulck.backend.infrastructure.repository.Entities.StreakEventEntity
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedStreakRepository : StreakRepository, KoinComponent {
    override suspend fun findByUserId(userId: UUID): Streak? = transaction {
        Streaks.select { Streaks.userId.eq(userId) }.firstOrNull()?.toDomain()
    }

    override suspend fun create(streak: Streak): Streak = transaction {
        val entity = StreakEntity.fromDomain(streak)
        entity.toDomain()
    }

    override suspend fun update(streak: Streak): Streak = transaction {
        val entity = StreakEntity.findById(id) ?: return streak
        entity.currentStreak = streak.currentStreak
        entity.longestStreak = streak.longestStreak
        entity.missedDays = streak.missedDays
        entity.status = streak.status
        entity.lastDepositAt = streak.lastDepositAt
        entity.lastMissedAt = streak.lastMissedAt
        entity.lockoutUntil = streak.lockoutUntil
        entity.updatedAt = streak.updatedAt
        entity.toDomain()
    }

    override suspend fun upsert(streak: Streak): Streak = transaction {
        (Streaks.select { Streaks.userId.eq(streak.userId) }.firstOrNull()
            ?.apply {
                currentStreak = streak.currentStreak
                longestStreak = streak.longestStreak
                missedDays = streak.missedDays
                status = streak.status
                lastDepositAt = streak.lastDepositAt
                lastMissedAt = streak.lastMissedAt
                lockoutUntil = streak.lockoutUntil
                updatedAt = streak.updatedAt
            }
            ?: StreakEntity.fromDomain(streak)
        ).toDomain()
    }

    override suspend fun recordEvent(event: StreakEvent): StreakEvent = transaction {
        val entity = StreakEventEntity.fromDomain(event)
        entity.toDomain()
    }

    override suspend fun getEventsByUserId(userId: UUID, limit: Int): List<com.sulck.backend.domain.entity.StreakEvent> = transaction {
        StreakEvents.select { StreakEvents.userId.eq(userId) }
            .orderByDescending(StreakEvents.createdAt)
            .limit(limit)
            .map { it.toDomain() }
    }
}