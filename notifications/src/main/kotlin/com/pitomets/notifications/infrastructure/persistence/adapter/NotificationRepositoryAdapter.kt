package com.pitomets.notifications.infrastructure.persistence.adapter

import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.infrastructure.persistence.entity.NotificationEntity
import com.pitomets.notifications.infrastructure.persistence.repository.NotificationJpaRepository
import org.springframework.stereotype.Component

@Component
class NotificationRepositoryAdapter(
    private val notificationJpaRepository: NotificationJpaRepository
) : NotificationRepository {

    override fun existsByEventId(eventId: Long): Boolean {
        return notificationJpaRepository.existsByEventId(eventId)
    }

    override fun save(notification: Notification) {
        val entity = NotificationEntity.fromDomain(notification)
        notificationJpaRepository.save(entity)
    }

    override fun findByUserId(userId: Long): List<Notification?> {
        return notificationJpaRepository.findByUserId(userId)
            .map { it?.toDomain() }
    }

    override fun findByEventId(eventId: Long): Notification? {
        return notificationJpaRepository.findByEventId(eventId)?.toDomain()
    }

    override fun findAll(): List<Notification> {
        return notificationJpaRepository.findAll()
            .map { it.toDomain() }
    }
}