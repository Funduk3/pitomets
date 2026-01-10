package com.pitomets.notifications.infrastructure.persistence.repository

import com.pitomets.notifications.infrastructure.persistence.entity.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationJpaRepository : JpaRepository<NotificationEntity, Long> {
    fun existsByEventId(eventId: Long): Boolean
    fun findByEventId(eventId: Long): NotificationEntity?
    fun findByUserId(userId: Long): List<NotificationEntity>
}
