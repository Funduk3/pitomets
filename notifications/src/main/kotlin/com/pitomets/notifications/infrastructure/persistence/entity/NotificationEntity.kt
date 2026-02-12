package com.pitomets.notifications.infrastructure.persistence.entity

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.MessageType
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.model.Status
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
data class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val channel: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = true)
    val messageType: MessageType?,

    @Column(columnDefinition = "TEXT", nullable = false)
    val payload: String,

    @Column(nullable = false)
    val status: String
) {
    // Преобразование в доменную модель
    fun toDomain(): Notification {
        return Notification(
            id = id,
            eventId = eventId,
            userId = userId,
            channel = Channel.valueOf(channel),
            messageType = messageType ?: MessageType.DEFAULT,
            payload = payload,
            status = Status.valueOf(status)
        )
    }

    companion object {
        // Преобразование из доменной модели
        fun fromDomain(notification: Notification): NotificationEntity {
            return NotificationEntity(
                id = notification.id,
                eventId = notification.eventId,
                userId = notification.userId,
                channel = notification.channel.name,
                messageType = notification.messageType,
                payload = notification.payload,
                status = notification.status.name
            )
        }
    }
}
