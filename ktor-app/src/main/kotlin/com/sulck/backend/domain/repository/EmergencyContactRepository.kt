package com.sulck.backend.domain.repository

import com.sulck.backend.domain.entity.EmergencyContact
import com.sulck.backend.domain.entity.EmergencyUnlockPolicy
import com.sulck.backend.domain.entity.EmergencyUnlockRequest
import com.sulck.backend.domain.entity.UnlockRequestStatus
import java.util.UUID

interface EmergencyContactRepository {
    fun findByUserId(userId: UUID): List<EmergencyContact>
    fun findById(id: UUID): EmergencyContact?
    fun upsert(contact: EmergencyContact)
    fun delete(id: UUID)
}

interface EmergencyUnlockPolicyRepository {
    fun findAll(): List<EmergencyUnlockPolicy>
    fun findById(id: UUID): EmergencyUnlockPolicy?
    fun findActive(): List<EmergencyUnlockPolicy>
    fun upsert(policy: EmergencyUnlockPolicy)
    fun delete(id: UUID)
}

interface EmergencyUnlockRequestRepository {
    fun findByUserId(userId: UUID): List<EmergencyUnlockRequest>
    fun findById(id: UUID): EmergencyUnlockRequest?
    fun findAllPending(): List<EmergencyUnlockRequest>
    fun upsert(request: EmergencyUnlockRequest)
    fun updateStatus(id: UUID, status: UnlockRequestStatus)
    fun delete(id: UUID)
}
