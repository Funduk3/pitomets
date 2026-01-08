package com.pitomets.notifications.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface OutboxJpaRepository : JpaRepository<OutboxEventEntity, Long> {

    @Query("""
        select e from OutboxEventEntity e
        where e.published = false
        order by e.createdAt
    """)
    fun findUnpublished(): List<OutboxEventEntity>
}
