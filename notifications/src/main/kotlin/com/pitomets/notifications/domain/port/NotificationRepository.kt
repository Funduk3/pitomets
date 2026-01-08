package com.pitomets.notifications.domain.port

import com.pitomets.notifications.domain.model.Notification
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationRepository {

    fun existsByEventId(eventId: Long): Boolean

    fun save(notification: Notification)

    // Добавляем методы для поиска, необходимые для тестов
    fun findByUserId(userId: Long): List<Notification?>

    fun findByEventId(eventId: Long): Notification?

    fun findAll(): List<Notification?>
}