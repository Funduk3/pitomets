package com.pitomets.notifications.infrastructure.persistence.adapter

import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.infrastructure.persistence.entity.NotificationEntity
import com.pitomets.notifications.infrastructure.persistence.repository.NotificationJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class NotificationRepositoryAdapter(
    private val notificationJpaRepository: NotificationJpaRepository,
    private val entityManager: EntityManager
) : NotificationRepository {

    override fun existsByEventId(eventId: Long): Boolean {
        return notificationJpaRepository.existsByEventId(eventId)
    }

    override fun save(notification: Notification): Notification {
        val entity = NotificationEntity.fromDomain(notification)
        val saved = notificationJpaRepository.save(entity)
        entityManager.flush() // Форсируем запись в БД
        return saved.toDomain()
    }

    override fun findByUserId(userId: Long): List<Notification> {
        return notificationJpaRepository.findByUserId(userId)
            .map { it.toDomain() }
    }

    override fun findByEventId(eventId: Long): Notification? {
        return notificationJpaRepository.findByEventId(eventId)?.toDomain()
    }

    override fun findAll(): List<Notification> {
        return notificationJpaRepository.findAll()
            .map { it.toDomain() }
    }

    // не нужен пока
    override fun updateStatus(eventId: Long, status: Status) {
        val current = notificationJpaRepository.findByEventId(eventId)
            ?: error("Notification with eventId=$eventId not found")

        val updated = current.copy(status = status.name)
        notificationJpaRepository.save(updated)
        entityManager.flush()
    }
}
