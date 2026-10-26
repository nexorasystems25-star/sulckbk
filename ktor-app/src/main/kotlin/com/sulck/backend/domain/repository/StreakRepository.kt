package com.sulck.backend.domain.repository

import com.sulck.backend.domain.entity.Streak
import com.sulck.backend.domain.entity.StreakEvent
import java.util.UUID

interface StreakRepository {
    suspend fun findByUserId(userId: UUID): Streak?
    suspend fun create(streak: Streak): Streak
    suspend fun update(streak: Streak): Streak
    suspend fun upsert(streak: Streak): Streak
    suspend fun recordEvent(event: StreakEvent): StreakEvent
    suspend fun getEventsByUserId(userId: UUID, limit: Int = 50): List<StreakEvent>
}