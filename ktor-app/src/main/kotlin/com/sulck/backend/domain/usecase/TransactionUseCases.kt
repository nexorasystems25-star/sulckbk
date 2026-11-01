package com.sulck.backend.domain.usecase

object TransactionUseCases {
    lateinit var depositUseCase: DepositUseCase
    lateinit var getTransactionHistoryUseCase: GetTransactionHistoryUseCase
    lateinit var processWebhookUseCase: ProcessWebhookUseCase
}