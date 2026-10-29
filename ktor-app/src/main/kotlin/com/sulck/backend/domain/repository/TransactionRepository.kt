package com.sulck.backend.domain.repository

import com.sulck.backend.domain.entity.Transaction
import java.util.UUID

interface TransactionRepository {
    suspend fun create(transaction: Transaction): Transaction
    suspend fun findById(id: UUID): Transaction?
    suspend fun findByUserId(userId: UUID, limit: Int = 50, offset: Int = 0): List<Transaction>
    suspend fun findByGoalId(goalId: UUID): List<Transaction>
    suspend fun findByProviderRef(providerRef: String): Transaction?
    suspend fun update(transaction: Transaction): Transaction
    suspend fun updateStatus(id: UUID, status: Transaction.Status): Boolean
    suspend fun countByUserId(userId: UUID): Long
    suspend fun sumByUserIdAndType(userId: UUID, type: Transaction.Type): Long
}