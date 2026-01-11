package com.pitomets.notifications.domain.port

import com.pitomets.notifications.domain.model.Notification
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository {

    fun existsByEventId(eventId: Long): Boolean

    fun save(notification: Notification): Notification

    // Добавляем методы для поиска, необходимые для тестов
    fun findByUserId(userId: Long): List<Notification>

    fun findByEventId(eventId: Long): Notification?

    fun findAll(): List<Notification>

    fun updateStatus(eventId: Long, status: com.pitomets.notifications.domain.model.Status)
}
