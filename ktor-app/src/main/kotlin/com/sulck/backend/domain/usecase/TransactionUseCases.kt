package com.sulck.backend.domain.usecase

import com.sulck.backend.config.PaystackConfig
import com.sulck.backend.domain.entity.Goal
import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.entity.Transaction
import com.sulck.backend.domain.entity.TransactionStatus
import com.sulck.backend.domain.entity.TransactionType
import com.sulck.backend.domain.repository.GoalRepository
import com.sulck.backend.domain.repository.TransactionRepository
import com.sulck.backend.domain.repository.UserRepository
import com.sulck.backend.infrastructure.service.PaystackService
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class DepositUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository,
    private val paystackService: PaystackService
) {
    data class Input(
        val userId: UUID,
        val goalId: UUID,
        val amount: Long,
        val provider: String, // PAYSTACK, MTN_MOMO, AIRTEL_TIGO, TELECEL
        val phoneNumber: String
    )

    data class Output(
        val transaction: Transaction,
        val paymentUrl: String?,
        val reference: String
    )

    operator fun invoke(input: Input): Output {
        val user = userRepository.findById(input.userId)
            ?: throw IllegalArgumentException("User not found")
        val goal = goalRepository.findById(input.goalId)
            ?: throw IllegalArgumentException("Goal not found")

        if (goal.status != GoalStatus.ACTIVE) {
            throw IllegalStateException("Goal is not active")
        }

        if (goal.userId != input.userId) {
            throw IllegalStateException("Goal does not belong to user")
        }

        // Generate unique reference
        val reference = "SULCK_${System.currentTimeMillis()}_${input.userId.toString().substring(0, 8)}"

        // Create pending transaction
        val transaction = Transaction(
            userId = input.userId,
            goalId = input.goalId,
            amount = input.amount,
            currency = goal.currency,
            type = TransactionType.DEPOSIT,
            status = TransactionStatus.PENDING,
            provider = input.provider,
            providerRef = reference,
            description = "Deposit to ${goal.title}"
        )

        val createdTransaction = transactionRepository.create(transaction)

        // Initialize Paystack payment
        val paymentUrl = paystackService.initializePayment(
            amount = input.amount,
            email = userRepository.findById(input.userId)?.email ?: "",
            reference = reference,
            phone = input.phoneNumber,
            provider = input.provider
        )

        return Output(createdTransaction, paymentUrl, reference)
    }
}

class GetTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(userId: UUID, limit: Int = 50, offset: Int = 0): List<Transaction> {
        return transactionRepository.findByUserId(userId, limit, offset)
    }
}

class ProcessWebhookUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository,
    private val paystackService: PaystackService
) {
    data class Input(
        val reference: String,
        val status: String, // success, failed
        val amount: Long,
        val provider: String,
        val payload: String
    )

    operator fun invoke(input: Input): Boolean {
        val transaction = transactionRepository.findByProviderRef(input.reference)
            ?: return false

        if (transaction.status == TransactionStatus.COMPLETED) {
            return true // Already processed
        }

        val isSuccess = input.status == "success"
        val newStatus = if (isSuccess) TransactionStatus.COMPLETED else TransactionStatus.FAILED

        transactionRepository.updateStatus(transaction.id, newStatus)
        transactionRepository.update(transaction.copy(
            status = newStatus,
            providerResponse = input.payload,
            completedAt = Instant.now()
        ))

        if (isSuccess) {
            // Update goal progress
            val goal = goalRepository.findById(transaction.goalId!!)
            if (goal != null) {
                val newProgress = goal.currentProgress + input.amount
                val newStatus = if (newProgress >= goal.targetAmount) GoalStatus.COMPLETED else goal.status
                goalRepository.update(goal.copy(
                    currentProgress = newProgress,
                    status = newStatus,
                    downPaymentPaid = true,
                    updatedAt = Instant.now()
                ))
            }
        }

        return isSuccess
    }
}