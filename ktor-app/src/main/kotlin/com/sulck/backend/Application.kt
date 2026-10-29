package com.sulck.backend

import com.sulck.backend.config.DatabaseConfig
import com.sulck.backend.config.JwtConfig
import com.sulck.backend.config.PaystackConfig
import com.sulck.backend.config.ServerConfig
import com.sulck.backend.di.module
import com.sulck.backend.domain.repository.*
import com.sulck.backend.domain.usecase.*
import com.sulck.backend.infrastructure.repository.ExposedGoalRepository
import com.sulck.backend.infrastructure.repository.ExposedUserRepository
import com.sulck.backend.infrastructure.repository.ExposedTransactionRepository
import com.sulck.backend.infrastructure.repository.ExposedStreakRepository
import com.sulck.backend.infrastructure.service.PaystackService
import com.sulck.backend.presentation.routes.configureRoutes
import org.jetbrains.koin.core.component.KoinComponent
import org.jetbrains.koin.core.inject
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.cors.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.statuspages.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.flywaydb.core.Flyway
import org.jetbrains.koin.core.context.startKoin
import org.jetbrains.koin.core.context.stopKoin
import org.jetbrains.koin.ktor.koin

fun main() {
    val logger = LoggerFactory.getLogger("SulckBackend")

    // Start Koin DI
    startKoin {
        modules(module)
    }

    // Run Flyway migrations
    val dbConfig = DatabaseConfig.load()
    Flyway.configure()
        .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
        .load()
        .migrate()

    // Initialize Exposed database connection
    val hikari = HikariDataSource().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.user
        password = dbConfig.password
        maximumPoolSize = dbConfig.poolSize
    }
    Database.connect(hikari, ThreadLocalTransactionManager())

    // Create tables if not exist
    transaction {
        com.sulck.backend.infrastructure.schema.Schema.createTables()
    }

    embeddedServer(Netty, configureApplication).start(wait = true)
}

fun Application.configureApplication() {
    install(Koin) { modules(module) }
    install(CallLogging) { level = io.ktor.util.logging.Level.INFO }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respondText("Internal Server Error: ${cause.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
        }
    }
    install(CallLogging) {
        level = io.ktor.util.logging.Level.INFO
    }
    install(CORS) {
        anyHost()
        allowMethods = setOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete, HttpMethod.Options)
        allowHeaders = setOf("Content-Type", "Authorization", "X-Requested-With")
        exposeHeaders = setOf("Content-Type")
        allowCredentials = true
    }
    install(Authentication) {
        jwt("auth-jwt") {
            val jwtConfig = JwtConfig.load()
            verifier(jwtConfig.verifier)
            validate { credential ->
                JWTPrincipal(credential.payload.getClaim("sub").asString() ?: "")
            }
        }
    }
    install(CallLogging)
    install(Logging) {
        level = io.ktor.util.logging.Level.INFO
    }

    val authUseCases by inject<AuthUseCases>()
    val goalUseCases by inject<GoalUseCases>()
    val transactionUseCases by inject<TransactionUseCases>()
    val streakUseCases by inject<StreakUseCases>()
    val adminUseCases by inject<AdminUseCases>()
    val emergencyUnlockUseCases by inject<EmergencyUnlockUseCases>()
    val paystackService by inject<PaystackService>()
    val paystackConfig = PaystackConfig.load()
    val getUserUseCase by inject<GetUserUseCase>()
    val createGoalUseCase by inject<CreateGoalUseCase>()
    val listGoalsUseCase by inject<ListGoalsUseCase>()
    val getTransactionHistoryUseCase by inject<GetTransactionHistoryUseCase>()

    configureRoutes(
        this,
        authUseCases = authUseCases,
        goalUseCases = goalUseCases,
        transactionUseCases = transactionUseCases,
        streakUseCases = streakUseCases,
        adminUseCases = adminUseCases,
        emergencyUnlockUseCases = emergencyUnlockUseCases,
        paystackService = paystackService,
        paystackConfig = paystackConfig,
        getUserUseCase = getUserUseCase,
        createGoalUseCase = createGoalUseCase,
        listGoalsUseCase = listGoalsUseCase,
        getTransactionHistoryUseCase = getTransactionHistoryUseCase
    )
}