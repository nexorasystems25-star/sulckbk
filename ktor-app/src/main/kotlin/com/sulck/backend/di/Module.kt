package com.sulck.backend.di

import com.sulck.backend.config.AppConfig
import com.sulck.backend.config.DatabaseConfig
import com.sulck.backend.config.JwtConfig
import com.sulck.backend.config.PaystackConfig
import com.sulck.backend.config.ServerConfig
import com.sulck.backend.domain.repository.GoalRepository
import com.sulck.backend.domain.repository.TransactionRepository
import com.sulck.backend.domain.repository.UserRepository
import com.sulck.backend.domain.repository.StreakRepository
import com.sulck.backend.domain.usecase.*
import com.sulck.backend.infrastructure.repository.ExposedGoalRepository
import com.sulck.backend.infrastructure.repository.ExposedTransactionRepository
import com.sulck.backend.infrastructure.repository.ExposedUserRepository
import com.sulck.backend.infrastructure.repository.ExposedStreakRepository
import com.sulck.backend.infrastructure.service.PaystackService
import com.sulck.backend.infrastructure.service.PaystackServiceImpl
import org.jetbrains.koin.core.module.Module
import org.jetbrains.koin.core.qualifier.named

val module: Module = module {
    // Config
    single<AppConfig> { AppConfig() }
    single<DatabaseConfig> { DatabaseConfig.load() }
    single<ServerConfig> { ServerConfig.load() }
    single<JwtConfig> { JwtConfig.load() }
    single<PaystackConfig> { PaystackConfig.load() }

    // Repositories
    single<UserRepository> { ExposedUserRepository() }
    single<GoalRepository> { ExposedGoalRepository() }
    single<TransactionRepository> { ExposedTransactionRepository() }
    single<StreakRepository> { ExposedStreakRepository() }

    // Services
    single<PaystackService> { PaystackServiceImpl(get()) }

    // Use Cases
    single { CreateUserUseCase(get()) }
    single { GetUserUseCase(get()) }
    single { AuthenticateUserUseCase(get(), get()) }
    single { RefreshTokenUseCase(get(), get()) }
    single { CreateGoalUseCase(get(), get()) }
    single { GetGoalUseCase(get()) }
    single { ListGoalsUseCase(get()) }
    single { DepositUseCase(get(), get(), get(), get()) }
    single { GetTransactionHistoryUseCase(get()) }
    single { ProcessWebhookUseCase(get(), get(), get(), get()) }
    single { GetStreakUseCase(get()) }
    single { UpdateStreakUseCase(get(), get()) }
    single { AdminUseCases(get(), get(), get()) }
}