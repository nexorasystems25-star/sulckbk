package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.Goal
import com.sulck.backend.domain.entity.GoalStatus
import com.sulck.backend.domain.entity.GoalTier
import com.sulck.backend.domain.entity.Transaction
import com.sulck.backend.domain.entity.TransactionStatus
import com.sulck.backend.domain.entity.TransactionType
import com.sulck.backend.domain.entity.User
import com.sulck.backend.domain.entity.Streak
import com.sulck.backend.domain.entity.StreakEvent
import com.sulck.backend.domain.entity.StreakEventType
import com.sulck.backend.domain.entity.StreakStatus
import com.sulck.backend.domain.entity.LockLevel
import com.sulck.backend.infrastructure.schema.Goals
import com.sulck.backend.infrastructure.schema.StreakEvents
import com.sulck.backend.infrastructure.schema.Streaks
import com.sulck.backend.infrastructure.schema.Transactions
import com.sulck.backend.infrastructure.schema.Users
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.getOrNull
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(Users)

    var email by Users.email
    var phone by Users.phone
    var fullName by Users.fullName
    var passwordHash by Users.passwordHash
    var isActive by Users.isActive
    var isVerified by Users.isVerified
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
    var lastLoginAt by Users.lastLoginAt

    fun toDomain(): User = User(
        id = this.id.value,
        email = email,
        phone = phone,
        fullName = fullName,
        passwordHash = passwordHash,
        isActive = isActive,
        isVerified = isVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastLoginAt = lastLoginAt
    )
}

class GoalEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GoalEntity>(Goals)

    var userId by Goals.userId
    var title by Goals.title
    var description by Goals.description
    var targetAmount by Goals.targetAmount
    var currency by Goals.currency
    var dailyCommitment by Goals.dailyCommitment
    var cadenceDays by Goals.cadenceDays
    var startDate by Goals.startDate
    var targetDate by Goals.targetDate
    var lockedAssetSerial by Goals.lockedAssetSerial
    var status by Goals.status
    var currentProgress by Goals.currentProgress
    var downPaymentPaid by Goals.downPaymentPaid
    var createdAt by Goals.createdAt
    var updatedAt by Goals.updatedAt

    fun toDomain(): Goal = Goal(
        id = this.id.value,
        userId = userId,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currency = currency,
        dailyCommitment = dailyCommitment,
        cadenceDays = cadenceDays,
        startDate = startDate,
        targetDate = targetDate,
        lockedAssetSerial = lockedAssetSerial,
        status = status,
        currentProgress = currentProgress,
        downPaymentPaid = downPaymentPaid,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(goal: Goal): GoalEntity {
            return GoalEntity.new {
                id = goal.id
                userId = goal.userId
                title = goal.title
                description = goal.description
                targetAmount = goal.targetAmount
                currency = goal.currency
                dailyCommitment = goal.dailyCommitment
                cadenceDays = goal.cadenceDays
                startDate = goal.startDate
                targetDate = goal.targetDate
                lockedAssetSerial = goal.lockedAssetSerial
                status = goal.status
                currentProgress = goal.currentProgress
                downPaymentPaid = goal.downPaymentPaid
                createdAt = goal.createdAt
                updatedAt = goal.updatedAt
            }
        }
    }
}

class TransactionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TransactionEntity>(Transactions)

    var userId by Transactions.userId
    var goalId by Transactions.goalId
    var amount by Transactions.amount
    var currency by Transactions.currency
    var type by Transactions.type
    var status by Transactions.status
    var provider by Transactions.provider
    var providerRef by Transactions.providerRef
    var providerResponse by Transactions.providerResponse
    var description by Transactions.description
    var metadata by Transactions.metadata
    var createdAt by Transactions.createdAt
    var completedAt by Transactions.completedAt

    fun toDomain(): Transaction = Transaction(
        id = this.id.value,
        userId = userId,
        goalId = goalId,
        amount = amount,
        currency = currency,
        type = type,
        status = status,
        provider = provider,
        providerRef = providerRef,
        providerResponse = providerResponse,
        description = description,
        metadata = metadata,
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity {
            return TransactionEntity.new {
                id = transaction.id
                userId = transaction.userId
                goalId = transaction.goalId
                amount = transaction.amount
                currency = transaction.currency
                type = transaction.type
                status = transaction.status
                provider = transaction.provider
                providerRef = transaction.providerRef
                providerResponse = transaction.providerResponse
                description = transaction.description
                metadata = transaction.metadata
                createdAt = transaction.createdAt
                completedAt = transaction.completedAt
            }
        }
    }
}

class StreakEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StreakEntity>(Streaks)

    var userId by Streaks.userId
    var currentStreak by Streaks.currentStreak
    var longestStreak by Streaks.longestStreak
    var missedDays by Streaks.missedDays
    var status by Streaks.status
    var lockLevel by Streaks.lockLevel
    var lastDepositAt by Streaks.lastDepositAt
    var lastMissedAt by Streaks.lastMissedAt
    var lockoutUntil by Streaks.lockoutUntil
    var tempLockTimestamp by Streaks.tempLockTimestamp
    var warningIssuedAt by Streaks.warningIssuedAt
    var appLockTimestamp by Streaks.appLockTimestamp
    var createdAt by Streaks.createdAt
    var updatedAt by Streaks.updatedAt

    fun toDomain(): Streak = Streak(
        id = this.id.value,
        userId = userId,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        missedDays = missedDays,
        status = status,
        lockLevel = lockLevel,
        lastDepositAt = lastDepositAt,
        lastMissedAt = lastMissedAt,
        lockoutUntil = lockoutUntil,
        tempLockTimestamp = tempLockTimestamp,
        warningIssuedAt = warningIssuedAt,
        appLockTimestamp = appLockTimestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(streak: Streak): StreakEntity {
            return StreakEntity.new {
                id = streak.id
                userId = streak.userId
                currentStreak = streak.currentStreak
                longestStreak = streak.longestStreak
                missedDays = streak.missedDays
                status = streak.status
                lockLevel = streak.lockLevel
                lastDepositAt = streak.lastDepositAt
                lastMissedAt = streak.lastMissedAt
                lockoutUntil = streak.lockoutUntil
                tempLockTimestamp = streak.tempLockTimestamp
                warningIssuedAt = streak.warningIssuedAt
                appLockTimestamp = streak.appLockTimestamp
                createdAt = streak.createdAt
                updatedAt = streak.updatedAt
            }
        }
    }
}

class StreakEventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StreakEventEntity>(StreakEvents)

    var userId by StreakEvents.userId
    var eventType by StreakEvents.eventType
    var streakBefore by StreakEvents.streakBefore
    var streakAfter by StreakEvents.streakAfter
    var metadata by StreakEvents.metadata
    var createdAt by StreakEvents.createdAt

    fun toDomain(): StreakEvent = StreakEvent(
        id = this.id.value,
        userId = userId,
        eventType = eventType,
        streakBefore = streakBefore,
        streakAfter = streakAfter,
        metadata = metadata,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(event: StreakEvent): StreakEventEntity {
            return StreakEventEntity.new {
                id = event.id
                userId = event.userId
                eventType = event.eventType
                streakBefore = event.streakBefore
                streakAfter = event.streakAfter
                metadata = event.metadata
                createdAt = event.createdAt
            }
        }
    }
}