package com.sulck.backend.presentation.routes

import com.sulck.backend.config.PaystackConfig
import com.sulck.backend.domain.usecase.*
import com.sulck.backend.infrastructure.service.PaystackService
import com.sulck.backend.presentation.dto.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import io.ktor.server.request.*
import java.util.UUID
import kotlinx.coroutines.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

fun Routing.configureRoutes(
    authUseCases: AuthUseCases,
    goalUseCases: GoalUseCases,
    transactionUseCases: TransactionUseCases,
    streakUseCases: StreakUseCases,
    adminUseCases: AdminUseCases,
    paystackService: PaystackService,
    paystackConfig: PaystackConfig,
    getUserUseCase: GetUserUseCase,
    createGoalUseCase: CreateGoalUseCase,
    listGoalsUseCase: ListGoalsUseCase,
    getTransactionHistoryUseCase: GetTransactionHistoryUseCase
) {
    // Health check
    get("/health") {
        call.respond(mapOf("status" to "ok", "service" to "sulck-backend"))
    }

    // Auth routes
    route("/auth") {
        post("/register") {
            val request = call.receive<AuthRequest.Register>()
            val user = authUseCases.CreateUserUseCase(
                request.email,
                request.phone,
                request.fullName,
                request.password
            )
            call.respond(AuthResponse.Register(user))
        }

        post("/login") {
            val request = call.receive<AuthRequest.Login>()
            val result = authUseCases.AuthenticateUserUseCase(request.emailOrPhone, request.password)
                ?: call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                ?: return@post
            call.respond(AuthResponse.Login(result))
        }

        post("/refresh") {
            val request = call.receive<AuthRequest.Refresh>()
            val result = authUseCases.RefreshTokenUseCase(request.refreshToken)
                ?: call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid refresh token"))
                ?: return@post
            call.respond(AuthResponse.Refresh(result))
        }

        // Protected route to get current user
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)
                val user = getUserUseCase(userId)
                user?.let { call.respond(AuthResponse.Me(it)) }
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            }
        }
    }

    // Goal routes (protected)
    authenticate("auth-jwt") {
        route("/goals") {
            post {
                val request = call.receive<GoalRequest.Create>()
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)

                val output = createGoalUseCase(
                    GoalUseCases.CreateGoalUseCase.Input(
                        userId = userId,
                        title = request.title,
                        description = request.description,
                        targetAmount = request.targetAmount,
                        currency = request.currency ?: "GHS",
                        durationDays = request.durationDays,
                        cadenceDays = request.cadenceDays ?: 1,
                        lockedAssetSerial = request.lockedAssetSerial
                    )
                )
                call.respond(HttpStatusCode.Created, GoalResponse.Create(output))
            }

            get {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true
                val goals = listGoalsUseCase(userId, activeOnly)
                call.respond(GoalResponse.List(goals))
            }

            route("/{goalId}") {
                get {
                    val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = UUID.fromString(principal.name)
                    val goalId = UUID.fromString(call.parameters["goalId"]!!)
                    val goal = goalUseCases.GetGoalUseCase(goalId)
                        ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("Goal not found"))
                        ?: return@get
                    if (goal.userId != userId) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                        return@get
                    }
                    call.respond(GoalResponse.Detail(goal))
                }
            }
        }

        // Transaction routes
        route("/transactions") {
            post("/deposit") {
                val request = call.receive<TransactionRequest.Deposit>()
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)

                val output = transactionUseCases.DepositUseCase.invoke(
                    TransactionUseCases.DepositUseCase.Input(
                        userId = userId,
                        goalId = UUID.fromString(request.goalId),
                        amount = request.amount,
                        provider = request.provider,
                        phoneNumber = request.phoneNumber
                    )
                )
                call.respond(HttpStatusCode.Created, TransactionResponse.Deposit(output))
            }

            get {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)
                val limit = call.request.queryParameters["limit"]?.toInt() ?: 50
                val offset = call.request.queryParameters["offset"]?.toInt() ?: 0
                val transactions = getTransactionHistoryUseCase(userId, limit, offset)
                call.respond(TransactionResponse.List(transactions))
            }
        }

        // Streak routes
        route("/streak") {
            get {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)
                val streak = streakUseCases.GetStreakUseCase(userId)
                call.respond(StreakResponse(streak))
            }
        }
    }

    // Admin routes (protected)
    authenticate("auth-jwt") {
        route("/admin") {
            get("/stats") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = UUID.fromString(principal.name)
                val user = getUserUseCase(userId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@get
                }

                val stats = adminUseCases()
                call.respond(stats)
            }
        }
    }

    // Public webhook (no auth)
    post("/webhook/paystack") {
        val payload = call.receiveText()
        val signature = call.request.headers["x-paystack-signature"] ?: ""
        val event = call.request.headers["x-paystack-event"] ?: ""

        // Verify Paystack signature
        val isValid = verifyPaystackSignature(payload, signature, paystackConfig.webhookSecret)
        if (!isValid) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid signature"))
            return@post
        }

        if (event == "charge.success" || event == "charge.failed") {
            val data = kotlinx.serialization.json.Json.decodeFromString<PaystackWebhookData>(payload)
            val result = transactionUseCases.ProcessWebhookUseCase.invoke(
                TransactionUseCases.ProcessWebhookUseCase.Input(
                    reference = data.data.reference,
                    status = if (event == "charge.success") "success" else "failed",
                    amount = data.data.amount,
                    provider = data.data.channel,
                    payload = payload
                )
            )
            call.respond(HttpStatusCode.OK, mapOf("status" to if (result) "processed" else "failed"))
        } else {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ignored"))
        }
    }
}

private fun verifyPaystackSignature(payload: String, signature: String, secret: String): Boolean {
    return try {
        val hmacSha512 = Mac.getInstance("HmacSHA512")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA512")
        hmacSha512.init(secretKey)
        val digest = hmacSha512.doFinal(payload.toByteArray())
        val expectedSignature = Base64.getEncoder().encodeToString(digest)
        expectedSignature == signature
    } catch (e: Exception) {
        false
    }
}

@Serializable
data class PaystackWebhookData(
    val event: String,
    val data: PaystackWebhookData.Data
) {
    @Serializable
    data class Data(
        val id: Long,
        val domain: String,
        val status: String,
        val reference: String,
        val amount: Long,
        val currency: String,
        val channel: String,
        val paid_at: String,
        val created_at: String,
        val customer: Customer,
        val authorization: Authorization
    ) {
        @Serializable
        data class Customer(
            val id: Long,
            val email: String,
            val phone: String?
        )
        @Serializable
        data class Authorization(
            val authorization_code: String,
            val card_type: String,
            val last4: String,
            val exp_month: String,
            val exp_year: String,
            val bin: String,
            val bank: String,
            val brand: String,
            val reusable: Boolean
        )
    }
}