package com.pitomets.notifications.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxJpaRepository : JpaRepository<OutboxEventEntity, Long> {
    @Query(
        value = """
        select *
        from outbox_events
        where published = false
        order by created_at
        limit :limit
        for update skip locked
    """,
        nativeQuery = true
    )
    fun findUnpublishedBatch(@Param("limit") limit: Int): List<OutboxEventEntity>
}
