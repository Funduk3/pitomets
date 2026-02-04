package com.pitomets.notifications.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface OutboxJpaRepository : JpaRepository<OutboxEventEntity, Long> {

    @Query(
        value = """
        select *
        from outbox_events
        where published = false
        order by created_at
        for update skip locked
    """,
        nativeQuery = true
    )
    fun findUnpublished(): List<OutboxEventEntity>
}
