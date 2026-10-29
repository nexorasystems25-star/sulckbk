package com.sulck.backend.domain.entity

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val goalId: UUID?,
    val amount: Long, // in minor units
    val currency: String = "GHS",
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val provider: String? = null, // PAYSTACK, MTN_MOMO, AIRTEL_TIGO, TELECEL
    val providerRef: String? = null,
    val providerResponse: String? = null,
    val description: String? = null,
    val metadata: String? = null, // JSON
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null
)

enum class TransactionType {
    DEPOSIT, WITHDRAWAL, DOWN_PAYMENT, REFUND, PENALTY, REWARD
}

enum class TransactionStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED
}