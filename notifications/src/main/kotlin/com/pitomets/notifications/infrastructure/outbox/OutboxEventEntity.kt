package com.pitomets.notifications.infrastructure.outbox

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "outbox_events")
data class OutboxEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    val eventData: String,

    @Column(nullable = false)
    var published: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)