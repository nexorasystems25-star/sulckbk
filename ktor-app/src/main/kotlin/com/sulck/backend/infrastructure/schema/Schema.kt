package com.sulck.backend.infrastructure.schema

import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.entity.GoalTier
import com.sulck.backend.domain.entity.StreakEventType
import com.sulck.backend.domain.entity.StreakStatus
import com.sulck.backend.domain.entity.TransactionStatus
import com.sulck.backend.domain.entity.TransactionType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.uuid
import org.jetbrains.exposed.sql.varchar
import org.jetbrains.exposed.sql.int
import org.jetbrains.exposed.sql.long
import org.jetbrains.exposed.sql.datetime
import org.jetbrains.exposed.sql.bool
import org.jetbrains.exposed.sql.enumByName

object Users : Table() {
    val id = uuid("id").primaryKey()
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 50).uniqueIndex()
    val fullName = varchar("full_name", 255)
    val passwordHash = varchar("password_hash", 255)
    val isActive = bool("is_active").default(true)
    val isVerified = bool("is_verified").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val lastLoginAt = datetime("last_login_at").nullable()
}

object Goals : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").index()
    val title = varchar("title", 255)
    val description = varchar("description", 1000)
    val targetAmount = long("target_amount")
    val currency = varchar("currency", 10).default("GHS")
    val dailyCommitment = long("daily_commitment")
    val cadenceDays = int("cadence_days").default(1)
    val startDate = datetime("start_date")
    val targetDate = datetime("target_date")
    val lockedAssetSerial = varchar("locked_asset_serial", 255).nullable()
    val status = enumByName<GoalStatus>("status").default(GoalStatus.ACTIVE)
    val currentProgress = long("current_progress").default(0)
    val downPaymentPaid = bool("down_payment_paid").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Transactions : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").index()
    val goalId = uuid("goal_id").nullable().index()
    val amount = long("amount")
    val currency = varchar("currency", 10).default("GHS")
    val type = enumByName<TransactionType>("type")
    val status = enumByName<TransactionStatus>("status").default(TransactionStatus.PENDING)
    val provider = varchar("provider", 50).nullable()
    val providerRef = varchar("provider_ref", 255).nullable().uniqueIndex()
    val providerResponse = varchar("provider_response", 1000).nullable()
    val description = varchar("description", 500).nullable()
    val metadata = varchar("metadata", 2000).nullable()
    val createdAt = datetime("created_at")
    val completedAt = datetime("completed_at").nullable()
}

object Streaks : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").uniqueIndex()
    val currentStreak = int("current_streak").default(0)
    val longestStreak = int("longest_streak").default(0)
    val missedDays = int("missed_days").default(0)
    val status = enumByName<StreakStatus>("status").default(StreakStatus.ACTIVE)
    val lastDepositAt = datetime("last_deposit_at").nullable()
    val lastMissedAt = datetime("last_missed_at").nullable()
    val lockoutUntil = datetime("lockout_until").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object StreakEvents : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").index()
    val eventType = enumByName<StreakEventType>("event_type")
    val streakBefore = int("streak_before")
    val streakAfter = int("streak_after")
    val metadata = varchar("metadata", 1000).nullable()
    val createdAt = datetime("created_at")
}

object Schema {
    fun createTables() {
        org.jetbrains.exposed.sql.SchemaUtils.create(Users, Goals, Transactions, Streaks, StreakEvents)
    }
}