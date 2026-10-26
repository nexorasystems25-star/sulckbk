package com.sulck.backend.infrastructure.repository

import com.sulck.backend.domain.entity.EmergencyContact
import com.sulck.backend.domain.repository.EmergencyContactRepository
import com.sulck.backend.infrastructure.schema.EmergencyContacts
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.koin.core.component.KoinComponent
import java.util.UUID

class ExposedEmergencyContactRepository : EmergencyContactRepository, KoinComponent {
    override fun findByUserId(userId: UUID): List<EmergencyContact> = transaction {
        EmergencyContacts.select { EmergencyContacts.userId.eq(userId) }
            .map { it.toDomain() }
    }

    override fun findById(id: UUID): EmergencyContact? = transaction {
        EmergencyContacts.select { EmergencyContacts.id.eq(id) }
            .firstOrNull()?.toDomain()
    }

    override fun upsert(contact: EmergencyContact) = transaction {
        (EmergencyContacts.select { EmergencyContacts.id.eq(contact.id) }.firstOrNull()
            ?.apply {
                userId = contact.userId
                contactType = contact.contactType.name
                contactValue = contact.contactValue
                status = contact.status.name
                priority = contact.priority.name
                reason = contact.reason
                notes = contact.notes
                resolvedAt = contact.resolvedAt
                updatedAt = contact.updatedAt
            }
            ?: EmergencyContactEntity.fromDomain(contact)
        ).toDomain()
    }

    override fun delete(id: UUID) = transaction {
        EmergencyContacts.select { EmergencyContacts.id.eq(id) }
            .firstOrNull()?.delete()
    }
}

class EmergencyContactEntity(id: UUID) {
    var userId by EmergencyContacts.userId
    var contactType by EmergencyContacts.contactType
    var contactValue by EmergencyContacts.contactValue
    var status by EmergencyContacts.status
    var priority by EmergencyContacts.priority
    var reason by EmergencyContacts.reason
    var notes by EmergencyContacts.notes
    var resolvedAt by EmergencyContacts.resolvedAt
    var createdAt by EmergencyContacts.createdAt
    var updatedAt by EmergencyContacts.updatedAt

    fun toDomain(): EmergencyContact = EmergencyContact(
        id = id,
        userId = userId,
        contactType = com.sulck.backend.domain.entity.ContactType.valueOf(contactType),
        contactValue = contactValue,
        status = com.sulck.backend.domain.entity.ContactStatus.valueOf(status),
        priority = com.sulck.backend.domain.entity.ContactPriority.valueOf(priority),
        reason = reason,
        notes = notes,
        resolvedAt = resolvedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(contact: EmergencyContact): EmergencyContactEntity {
            return EmergencyContactEntity(contact.id).apply {
                userId = contact.userId
                contactType = contact.contactType.name
                contactValue = contact.contactValue
                status = contact.status.name
                priority = contact.priority.name
                reason = contact.reason
                notes = contact.notes
                resolvedAt = contact.resolvedAt
                createdAt = contact.createdAt
                updatedAt = contact.updatedAt
            }
        }
    }
}
