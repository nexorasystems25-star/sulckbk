package com.sulck.backend.domain.usecase

object TransactionUseCases {
    lateinit var depositUseCase: DepositUseCase
    lateinit var getTransactionHistoryUseCase: GetTransactionHistoryUseCase
    lateinit var processWebhookUseCase: ProcessWebhookUseCase
}

// Keep the original use case classes for compatibility
class DepositUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository,
    private val paystackService: PaystackService
)

class GetTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
)

class ProcessWebhookUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository,
    private val paystackService: PaystackService
)