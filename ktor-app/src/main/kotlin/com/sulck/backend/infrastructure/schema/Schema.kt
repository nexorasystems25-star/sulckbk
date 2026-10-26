package com.sulck.backend.infrastructure.schema

import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.entity.GoalTier
import com.sulck.backend.domain.entity.StreakEventType
import com.sulck.backend.domain.entity.StreakStatus
import com.sulck.backend.domain.entity.LockLevel
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
    val lockLevel = enumByName<LockLevel>("lock_level").default(LockLevel.NONE)
    val lastDepositAt = datetime("last_deposit_at").nullable()
    val lastMissedAt = datetime("last_missed_at").nullable()
    val lockoutUntil = datetime("lockout_until").nullable()
    val tempLockTimestamp = datetime("temp_lock_timestamp").nullable()
    val warningIssuedAt = datetime("warning_issued_at").nullable()
    val appLockTimestamp = datetime("app_lock_timestamp").nullable()
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

object EmergencyContacts : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").index()
    val contactType = varchar("contact_type", 50)
    val contactValue = varchar("contact_value", 255)
    val status = varchar("status", 50).default("OPEN")
    val priority = varchar("priority", 50).default("NORMAL")
    val reason = varchar("reason", 500).nullable()
    val notes = varchar("notes", 1000).nullable()
    val resolvedAt = datetime("resolved_at").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object EmergencyUnlockPolicies : Table() {
    val id = uuid("id").primaryKey()
    val name = varchar("name", 255)
    val description = varchar("description", 1000)
    val maxUnlocksPerMonth = int("max_unlocks_per_month")
    val maxUnlockDurationHours = int("max_unlock_duration_hours")
    val requiresApproval = bool("requires_approval").default(true)
    val allowedFromLockLevels = varchar("allowed_from_lock_levels", 500)
    val allowedToLockLevels = varchar("allowed_to_lock_levels", 500)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object EmergencyUnlockRequests : Table() {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").index()
    val contactId = uuid("contact_id").nullable().index()
    val policyId = uuid("policy_id").index()
    val requestedBy = uuid("requested_by")
    val fromLockLevel = varchar("from_lock_level", 50)
    val toLockLevel = varchar("to_lock_level", 50)
    val reason = varchar("reason", 1000)
    val durationHours = int("duration_hours").nullable()
    val status = varchar("status", 50).default("PENDING")
    val approvedBy = uuid("approved_by").nullable()
    val approvedAt = datetime("approved_at").nullable()
    val rejectedBy = uuid("rejected_by").nullable()
    val rejectedAt = datetime("rejected_at").nullable()
    val rejectionReason = varchar("rejection_reason", 500).nullable()
    val effectiveAt = datetime("effective_at").nullable()
    val expiresAt = datetime("expires_at").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Schema {
    fun createTables() {
        org.jetbrains.exposed.sql.SchemaUtils.create(
            Users,
            Goals,
            Transactions,
            Streaks,
            StreakEvents,
            EmergencyContacts,
            EmergencyUnlockPolicies,
            EmergencyUnlockRequests
        )
    }
}