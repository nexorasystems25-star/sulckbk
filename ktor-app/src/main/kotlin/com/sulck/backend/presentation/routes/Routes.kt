package com.sulck.backend.presentation.routes

import com.sulck.backend.config.PaystackConfig
import com.sulck.backend.domain.entity.User
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
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import java.util.UUID
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

fun Routing.configureRoutes(
    authUseCases: AuthUseCases,
    createGoalUseCase: CreateGoalUseCase,
    listGoalsUseCase: ListGoalsUseCase,
    getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    streakUseCase: GetStreakUseCase,
    adminUseCases: AdminUseCases,
    emergencyUnlockUseCases: EmergencyUnlockUseCases,
    paystackService: PaystackService,
    paystackConfig: PaystackConfig,
    getUserUseCase: (UUID) -> User?
) {
    // Health check
    get("/health") {
        call.respond(mapOf("status" to "ok", "service" to "sulck-backend"))
    }

    // Auth routes
    route("/auth") {
        post("/register") {
            val request = call.receive<AuthRequest.Register>()
            val user = authUseCases.createUserUseCase(
                request.email,
                request.phone,
                request.fullName,
                request.password
            )
            call.respond(AuthResponse.Register(user))
        }

        post("/login") {
            val request = call.receive<AuthRequest.Login>()
            val result = authUseCases.authenticateUserUseCase(request.emailOrPhone, request.password)
            call.respond(AuthResponse.Login(result))
        }

        post("/refresh") {
            val request = call.receive<AuthRequest.Refresh>()
            val result = authUseCases.refreshTokenUseCase(request.refreshToken)
            call.respond(AuthResponse.Refresh(result))
        }

        // Protected route to get current user
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
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
                val userId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))

                val output = createGoalUseCase(
                    createGoalUseCase.Input(
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
                val userId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true
                val goals = listGoalsUseCase(userId, activeOnly)
                call.respond(GoalResponse.List(goals))
            }

            route("/{goalId}") {
                get {
                    val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.subject?.let { UUID.fromString(it) }
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                    val goalId = UUID.fromString(call.parameters["goalId"]!!)
                    val goal = getUserUseCase(userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    if (goal.userId != userId) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                        return@get
                    }
                    // Note: In a full implementation, use case would fetch the goal
                    call.respond(GoalResponse.Detail(User(id = userId)))
                }
            }
        }
    }

    // Transaction routes
    route("/transactions") {
        post("/deposit") {
            val request = call.receive<TransactionRequest.Deposit>()
            val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.subject?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))

            // Use case invocation with Input
            val output = getTransactionHistoryUseCase(userId, 50, 0)
            call.respond(TransactionResponse.List(getTransactionHistoryUseCase(userId, 50, 0)))
        }

        get {
            val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.subject?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 50
            val offset = call.request.queryParameters["offset"]?.toInt() ?: 0
            call.respond(TransactionResponse.List(emptyList()))
        }
    }

    // Streak routes
    route("/streak") {
        get {
            val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.subject?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
            val streak = streakUseCase(userId)
            val streakSummary = streak?.let {
                StreakSummary(
                    id = it.id,
                    userId = it.userId,
                    currentStreak = it.currentStreak,
                    longestStreak = it.longestStreak,
                    missedDays = it.missedDays,
                    status = it.status,
                    lockLevel = it.lockLevel,
                    lastDepositAt = it.lastDepositAt,
                    lastMissedAt = it.lastMissedAt,
                    lockoutUntil = it.lockoutUntil,
                    tempLockTimestamp = it.tempLockTimestamp,
                    appLockTimestamp = it.appLockTimestamp,
                    warningIssuedAt = it.warningIssuedAt,
                    isLocked = it.isLocked,
                    isAppLocked = it.isAppLocked,
                    isTempLocked = it.isTempLocked,
                    isFullyLocked = it.isFullyLocked
                )
            }
            call.respond(StreakResponse(streakSummary))
        }
    }

    // Admin routes (protected)
    authenticate("auth-jwt") {
        route("/admin") {
            get("/stats") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val user = getUserUseCase(userId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@get
                }

                call.respond(mapOf("message" to "Admin stats placeholder"))
            }

            get("/devices") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val userId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val user = getUserUseCase(userId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@get
                }

                call.respond(mapOf("message" to "Device status placeholder"))
            }

            post("/emergency-unlock") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val adminUserId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val adminUser = getUserUseCase(adminUserId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!adminUser.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@post
                }

                val input = call.receive<EmergencyUnlockInput>()
                val request = emergencyUnlockUseCases.CreateEmergencyUnlockRequestUseCase(
                    emergencyUnlockUseCases.CreateEmergencyUnlockRequestUseCase.Input(
                        userId = input.userId,
                        policyId = input.policyId,
                        requestedBy = adminUserId,
                        fromLockLevel = input.fromLockLevel,
                        toLockLevel = input.toLockLevel,
                        reason = input.reason,
                        durationHours = input.durationHours,
                        contactId = input.contactId
                    )
                )

                val summary = EmergencyUnlockRequestSummary(
                    id = request.id,
                    userId = request.userId,
                    policyId = request.policyId,
                    requestedBy = request.requestedBy,
                    fromLockLevel = request.fromLockLevel,
                    toLockLevel = request.toLockLevel,
                    reason = request.reason,
                    durationHours = request.durationHours,
                    status = request.status.name,
                    approvedBy = request.approvedBy,
                    approvedAt = request.approvedAt,
                    rejectedBy = request.rejectedBy,
                    rejectedAt = request.rejectedAt,
                    effectiveAt = request.effectiveAt,
                    expiresAt = request.expiresAt,
                    createdAt = request.createdAt
                )
                call.respond(EmergencyUnlockRequestResponse(summary))
            }

            post("/emergency-unlock/{requestId}/approve") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val adminUserId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val user = getUserUseCase(adminUserId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@post
                }

                call.respond(mapOf("message" to "Approve placeholder"))
            }

            post("/emergency-unlock/{requestId}/apply") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val adminUserId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val user = getUserUseCase(adminUserId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@post
                }

                call.respond(mapOf("message" to "Apply placeholder"))
            }

            post("/emergency-unlock/{requestId}/reject") {
                val principal = call.authentication.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                val adminUserId = principal?.subject?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication"))
                val user = getUserUseCase(adminUserId)
                    ?: call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))

                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
                    return@post
                }

                val input = call.receive<RejectUnlockInput>()
                val request = emergencyUnlockUseCases.RejectEmergencyUnlockUseCase(requestId = UUID.fromString(call.parameters["requestId"] ?: ""), adminUserId = adminUserId, reason = input.reason)

                val summary = EmergencyUnlockRequestSummary(
                    id = request.id,
                    userId = request.userId,
                    policyId = request.policyId,
                    requestedBy = request.requestedBy,
                    fromLockLevel = request.fromLockLevel,
                    toLockLevel = request.toLockLevel,
                    reason = request.reason,
                    durationHours = request.durationHours,
                    status = request.status.name,
                    approvedBy = request.approvedBy,
                    approvedAt = request.approvedAt,
                    rejectedBy = request.rejectedBy,
                    rejectedAt = request.rejectedAt,
                    effectiveAt = request.effectiveAt,
                    expiresAt = request.expiresAt,
                    createdAt = request.createdAt
                )
                call.respond(EmergencyUnlockRequestResponse(summary))
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
            val data = Json.decodeFromString<PaystackWebhookData>(payload)
            call.respond(HttpStatusCode.OK, mapOf("status" to "processed"))
        } else {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ignored"))
        }
    }
}

private fun verifyPaystackSignature(payload: String, signature: String, secret: String): Boolean {
    return try {
        val hmacSha512 = javax.crypto.Mac.getInstance("HmacSHA512")
        val secretKey = javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA512")
        hmacSha512.init(secretKey)
        val digest = hmacSha512.doFinal(payload.toByteArray())
        val expectedSignature = java.util.Base64.getEncoder().encodeToString(digest)
        expectedSignature == signature
    } catch (e: Exception) {
        false
    }
}