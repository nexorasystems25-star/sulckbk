package com.sulck.backend.domain.repository

import com.sulck.backend.domain.entity.Goal
import java.util.UUID

interface GoalRepository {
    suspend fun create(goal: Goal): Goal
    suspend fun findById(id: UUID): Goal?
    suspend fun findByUserId(userId: UUID): List<Goal>
    suspend fun findActiveByUserId(userId: UUID): List<Goal>
    suspend fun update(goal: Goal): Goal
    suspend fun updateProgress(id: UUID, newProgress: Long): Boolean
    suspend fun updateStatus(id: UUID, status: Goal.Status): Boolean
    suspend fun delete(id: UUID): Boolean
}