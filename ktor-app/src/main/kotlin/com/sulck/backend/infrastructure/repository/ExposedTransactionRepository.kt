package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.Transaction
import com.sulck.backend.domain.entity.TransactionStatus
import com.sulck.backend.domain.entity.TransactionType
import com.sulck.backend.domain.repository.TransactionRepository
import com.sulck.backend.infrastructure.repository.Entities.TransactionEntity
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedTransactionRepository : TransactionRepository, KoinComponent {
    override suspend fun create(transaction: Transaction): Transaction = transaction {
        val entity = TransactionEntity.fromDomain(transaction)
        entity.toDomain()
    }

    override suspend fun findById(id: UUID): Transaction? = transaction {
        TransactionEntity.findById(id)?.toDomain()
    }

    override suspend fun findByUserId(userId: UUID, limit: Int, offset: Int): List<Transaction> = transaction {
        Transactions.select { Transactions.userId.eq(userId) }
            .orderByDescending(Transactions.createdAt)
            .limit(limit)
            .offset(offset)
            .map { it.toDomain() }
    }

    override suspend fun findByGoalId(goalId: UUID): List<Transaction> = transaction {
        Transactions.select { Transactions.goalId.eq(goalId) }
            .orderByDescending(Transactions.createdAt)
            .map { it.toDomain() }
    }

    override suspend fun findByProviderRef(providerRef: String): Transaction? = transaction {
        (Transactions.select { Transactions.providerRef.eq(providerRef) }.firstOrNull())?.toDomain()
    }

    override suspend fun update(transaction: Transaction): Transaction = transaction {
        val entity = TransactionEntity.findById(id) ?: return transaction
        entity.amount = transaction.amount
        entity.currency = transaction.currency
        entity.type = transaction.type
        entity.status = transaction.status
        entity.provider = transaction.provider
        entity.providerRef = transaction.providerRef
        entity.providerResponse = transaction.providerResponse
        entity.description = transaction.description
        entity.metadata = transaction.metadata
        entity.completedAt = transaction.completedAt
        entity.toDomain()
    }

    override suspend fun updateStatus(id: UUID, status: TransactionStatus): Boolean = transaction {
        val entity = TransactionEntity.findById(id) ?: return false
        entity.status = status
        if (status == TransactionStatus.COMPLETED) {
            entity.completedAt = java.time.Instant.now()
        }
        true
    }

    override suspend fun countByUserId(userId: UUID): Long = transaction {
        Transactions.select { Transactions.userId.eq(userId) }.count()
    }

    override suspend fun sumByUserIdAndType(userId: UUID, type: TransactionType): Long = transaction {
        Transactions.select {
            Transactions.userId.eq(userId) and Transactions.type.eq(type) and Transactions.status.eq(TransactionStatus.COMPLETED)
        }.sumBy { it[Transactions.amount] } ?: 0L
    }
}