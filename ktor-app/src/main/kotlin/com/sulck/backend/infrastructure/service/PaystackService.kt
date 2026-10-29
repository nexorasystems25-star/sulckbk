package com.sulck.backend.infrastructure.service

import com.sulck.backend.config.PaystackConfig
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.*

interface PaystackService {
    suspend fun initializePayment(
        amount: Long,
        email: String,
        reference: String,
        phone: String,
        provider: String
    ): String?

    suspend fun verifyPayment(reference: String): PaystackVerificationResponse?

    suspend fun getBanks(): List<PaystackBank>
}

class PaystackServiceImpl @Inject constructor(
    private val config: PaystackConfig
) : PaystackService {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            url(config.baseUrl)
            header(HttpHeaders.Authorization, "Bearer ${config.secretKey}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
    }

    @Serializable
    data class InitializeRequest(
        val amount: Long,
        val email: String,
        val reference: String,
        val currency: String = "GHS",
        val channels: List<String> = listOf("mobile_money"),
        val metadata: Map<String, String> = emptyMap()
    )

    @Serializable
    data class InitializeResponse(
        val status: Boolean,
        val message: String,
        val data: InitializeData?
    )

    @Serializable
    data class InitializeData(
        val authorization_url: String,
        val access_code: String,
        val reference: String
    )

    @Serializable
    data class VerificationResponse(
        val status: Boolean,
        val message: String,
        val data: VerificationData?
    )

    @Serializable
    data class VerificationData(
        val id: Long,
        val amount: Long,
        val currency: String,
        val status: String,
        val reference: String,
        val channel: String,
        val paid_at: String,
        val created_at: String,
        val customer: Customer,
        val authorization: Authorization
    )

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

    @Serializable
    data class PaystackVerificationResponse(
        val success: Boolean,
        val amount: Long,
        val currency: String,
        val reference: String,
        val status: String,
        val channel: String,
        val paidAt: String,
        val customerEmail: String,
        val authorizationCode: String
    )

    @Serializable
    data class Bank(
        val id: Long,
        val name: String,
        val slug: String,
        val code: String,
        val longcode: String,
        val gateway: String?,
        val pay_with_bank: Boolean,
        val supports_transfer: Boolean,
        val active: Boolean,
        val is_deleted: Boolean,
        val country: String,
        val currency: String,
        val type: String,
        val createdAt: String,
        val updatedAt: String
    )

    override suspend fun initializePayment(
        amount: Long,
        email: String,
        reference: String,
        phone: String,
        provider: String
    ): String? {
        val request = InitializeRequest(
            amount = amount,
            email = email,
            reference = reference,
            metadata = mapOf(
                "phone" to phone,
                "provider" to provider
            )
        )

        val response = client.post<InitializeResponse>("/transaction/initialize") {
            setBody(request)
        }

        return if (response.status && response.data != null) {
            response.data!!.authorization_url
        } else null
    }

    override suspend fun verifyPayment(reference: String): PaystackVerificationResponse? {
        val response = client.get<VerificationResponse>("/transaction/verify/$reference")

        return if (response.status && response.data != null) {
            val data = response.data!!
            PaystackVerificationResponse(
                success = data.status == "success",
                amount = data.amount,
                currency = data.currency,
                reference = data.reference,
                status = data.status,
                channel = data.channel,
                paidAt = data.paid_at,
                customerEmail = data.customer.email,
                authorizationCode = data.authorization.authorization_code
            )
        } else null
    }

    override suspend fun getBanks(): List<PaystackBank> {
        val response = client.get<List<PaystackBank>>("/bank")
        return response
    }
}

@Serializable
data class PaystackBank(
    val id: Long,
    val name: String,
    val slug: String,
    val code: String,
    val longcode: String,
    val gateway: String?,
    val pay_with_bank: Boolean,
    val supports_transfer: Boolean,
    val active: Boolean,
    val is_deleted: Boolean,
    val country: String,
    val currency: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String
)