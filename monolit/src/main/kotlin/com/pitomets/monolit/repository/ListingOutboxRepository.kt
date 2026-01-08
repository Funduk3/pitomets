package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.ListingOutbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ListingOutboxRepository : JpaRepository<ListingOutbox, Long> {

    @Query(
        value = """
            SELECT *
            FROM listing_outbox
            WHERE processed_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findBatchForProcessing(@Param("limit") limit: Int): List<ListingOutbox>

    @Modifying
    @Transactional
    @Query("DELETE FROM ListingOutbox o WHERE o.id IN :ids")
    fun deleteBatchByIds(ids: List<Long>)

    @Query(
        value = """
            SELECT o.id 
            FROM listing_outbox o
            WHERE o.processed_at IS NOT NULL
            ORDER BY o.created_at
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findProcessedIds(limit: Int): List<Long>

    @Modifying
    @Transactional
    @Query("UPDATE ListingOutbox o SET o.processedAt = :processedAt WHERE o.id = :id")
    fun markProcessed(@Param("id") id: Long, @Param("processedAt") processedAt: Instant = Instant.now())

    @Modifying
    @Transactional
    @Query("UPDATE ListingOutbox o SET o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    fun incrementRetry(@Param("id") id: Long)
}
