package com.sulck.backend.domain.usecase

import com.sulck.backend.domain.entity.EmergencyContact
import com.sulck.backend.domain.entity.EmergencyUnlockPolicy
import com.sulck.backend.domain.entity.EmergencyUnlockRequest
import com.sulck.backend.domain.entity.LockLevel
import com.sulck.backend.domain.entity.Streak
import com.sulck.backend.domain.entity.UnlockRequestStatus
import com.sulck.backend.domain.repository.EmergencyContactRepository
import com.sulck.backend.domain.repository.EmergencyUnlockPolicyRepository
import com.sulck.backend.domain.repository.EmergencyUnlockRequestRepository
import com.sulck.backend.domain.repository.StreakRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateEmergencyContactUseCase @Inject constructor(
    private val emergencyContactRepository: EmergencyContactRepository
) {
    operator fun invoke(contact: EmergencyContact): EmergencyContact {
        emergencyContactRepository.upsert(contact)
        return contact
    }
}

class GetEmergencyContactsUseCase @Inject constructor(
    private val emergencyContactRepository: EmergencyContactRepository
) {
    operator fun invoke(userId: UUID): List<EmergencyContact> {
        return emergencyContactRepository.findByUserId(userId)
    }
}

class CreateEmergencyUnlockRequestUseCase @Inject constructor(
    private val emergencyUnlockRequestRepository: EmergencyUnlockRequestRepository,
    private val emergencyUnlockPolicyRepository: EmergencyUnlockPolicyRepository
) {
    data class Input(
        val userId: UUID,
        val policyId: UUID,
        val requestedBy: UUID,
        val fromLockLevel: String,
        val toLockLevel: String,
        val reason: String,
        val durationHours: Int? = null,
        val contactId: UUID? = null
    )

    operator fun invoke(input: Input): EmergencyUnlockRequest {
        val policy = emergencyUnlockPolicyRepository.findById(input.policyId)
            ?: throw IllegalArgumentException("Policy not found")

        if (!policy.isActive) {
            throw IllegalArgumentException("Policy is not active")
        }

        if (!policy.allowedFromLockLevels.contains(input.fromLockLevel)) {
            throw IllegalArgumentException("Cannot unlock from this lock level")
        }

        if (!policy.allowedToLockLevels.contains(input.toLockLevel)) {
            throw IllegalArgumentException("Cannot unlock to this lock level")
        }

        val request = EmergencyUnlockRequest(
            userId = input.userId,
            contactId = input.contactId,
            policyId = input.policyId,
            requestedBy = input.requestedBy,
            fromLockLevel = input.fromLockLevel,
            toLockLevel = input.toLockLevel,
            reason = input.reason,
            durationHours = input.durationHours,
            status = if (policy.requiresApproval) UnlockRequestStatus.PENDING else UnlockRequestStatus.APPROVED
        )

        emergencyUnlockRequestRepository.upsert(request)
        return request
    }
}

class ApproveEmergencyUnlockUseCase @Inject constructor(
    private val emergencyUnlockRequestRepository: EmergencyUnlockRequestRepository
) {
    operator fun invoke(requestId: UUID, approvedBy: UUID): EmergencyUnlockRequest {
        val request = emergencyUnlockRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Request not found")

        if (request.status != UnlockRequestStatus.PENDING) {
            throw IllegalArgumentException("Request is not pending")
        }

        val updatedRequest = request.copy(
            status = UnlockRequestStatus.APPROVED,
            approvedBy = approvedBy,
            approvedAt = Instant.now()
        )

        emergencyUnlockRequestRepository.upsert(updatedRequest)
        return updatedRequest
    }
}

class ApplyEmergencyUnlockUseCase @Inject constructor(
    private val emergencyUnlockRequestRepository: EmergencyUnlockRequestRepository,
    private val streakRepository: StreakRepository
) {
    operator fun invoke(requestId: UUID): EmergencyUnlockRequest {
        val request = emergencyUnlockRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Request not found")

        if (request.status != UnlockRequestStatus.APPROVED) {
            throw IllegalArgumentException("Request is not approved")
        }

        val streak = streakRepository.findByUserId(request.userId)
            ?: throw IllegalArgumentException("Streak not found")

        val newLockLevel = try {
            LockLevel.valueOf(request.toLockLevel)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid lock level")
        }

        val now = Instant.now()
        val expiresAt = request.durationHours?.let { now.plusSeconds(it * 3600L) }

        val updatedStreak = streak.copy(
            lockLevel = newLockLevel,
            updatedAt = now
        )

        streakRepository.upsert(updatedStreak)

        val updatedRequest = request.copy(
            status = UnlockRequestStatus.APPLIED,
            effectiveAt = now,
            expiresAt = expiresAt,
            updatedAt = now
        )

        emergencyUnlockRequestRepository.upsert(updatedRequest)
        return updatedRequest
    }
}

class RejectEmergencyUnlockUseCase @Inject constructor(
    private val emergencyUnlockRequestRepository: EmergencyUnlockRequestRepository
) {
    operator fun invoke(requestId: UUID, rejectedBy: UUID, reason: String): EmergencyUnlockRequest {
        val request = emergencyUnlockRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Request not found")

        if (request.status != UnlockRequestStatus.PENDING) {
            throw IllegalArgumentException("Request is not pending")
        }

        val updatedRequest = request.copy(
            status = UnlockRequestStatus.REJECTED,
            rejectedBy = rejectedBy,
            rejectedAt = Instant.now(),
            rejectionReason = reason
        )

        emergencyUnlockRequestRepository.upsert(updatedRequest)
        return updatedRequest
    }
}

class GetAllDevicesStatusUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val userRepository: com.sulck.backend.domain.repository.UserRepository,
    private val goalRepository: com.sulck.backend.domain.repository.GoalRepository
) {
    data class DeviceStatus(
        val userId: UUID,
        val userEmail: String,
        val userPhone: String,
        val lockLevel: String,
        val missedDays: Int,
        val currentStreak: Int,
        val status: String,
        val isLocked: Boolean,
        val isAppLocked: Boolean,
        val isTempLocked: Boolean,
        val isFullyLocked: Boolean,
        val lastDepositAt: Instant?,
        val lastMissedAt: Instant?,
        val lockoutUntil: Instant?,
        val tempLockTimestamp: Instant?,
        val appLockTimestamp: Instant?,
        val lockedAssetSerial: String?,
        val goalProgress: Double?,
        val goalTarget: Long?,
        val goalRemaining: Long?
    )

    suspend operator fun invoke(): List<DeviceStatus> {
        val allStreaks = streakRepository.findAll()
        return allStreaks.map { streak ->
            val user = userRepository.findById(streak.userId)
            val goals = goalRepository.findByUserId(streak.userId)
            val activeGoal = goals.firstOrNull { it.status == com.sulck.backend.domain.entity.GoalStatus.ACTIVE }
            
            DeviceStatus(
                userId = streak.userId,
                userEmail = user?.email ?: "",
                userPhone = user?.phone ?: "",
                lockLevel = streak.lockLevel.name,
                missedDays = streak.missedDays,
                currentStreak = streak.currentStreak,
                status = streak.status.name,
                isLocked = streak.isLocked,
                isAppLocked = streak.isAppLocked,
                isTempLocked = streak.isTempLocked,
                isFullyLocked = streak.isFullyLocked,
                lastDepositAt = streak.lastDepositAt,
                lastMissedAt = streak.lastMissedAt,
                lockoutUntil = streak.lockoutUntil,
                tempLockTimestamp = streak.tempLockTimestamp,
                appLockTimestamp = streak.appLockTimestamp,
                lockedAssetSerial = activeGoal?.lockedAssetSerial,
                goalProgress = activeGoal?.progressPercent,
                goalTarget = activeGoal?.targetAmount,
                goalRemaining = activeGoal?.let { it.targetAmount - it.currentProgress }
            )
        }
    }
}

class EmergencyUnlockUseCases(
    val CreateEmergencyContactUseCase: CreateEmergencyContactUseCase,
    val GetEmergencyContactsUseCase: GetEmergencyContactsUseCase,
    val CreateEmergencyUnlockRequestUseCase: CreateEmergencyUnlockRequestUseCase,
    val ApproveEmergencyUnlockUseCase: ApproveEmergencyUnlockUseCase,
    val ApplyEmergencyUnlockUseCase: ApplyEmergencyUnlockUseCase,
    val RejectEmergencyUnlockUseCase: RejectEmergencyUnlockUseCase,
    val GetAllDevicesStatusUseCase: GetAllDevicesStatusUseCase
)
