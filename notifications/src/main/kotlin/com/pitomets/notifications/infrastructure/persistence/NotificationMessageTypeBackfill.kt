package com.pitomets.notifications.infrastructure.persistence

import com.pitomets.notifications.domain.model.MessageType
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationMessageTypeBackfill(
    private val jdbc: JdbcTemplate
) {

    private val log = LoggerFactory.getLogger(NotificationMessageTypeBackfill::class.java)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun run() {
        if (!messageTypeColumnExists()) {
            log.warn("Column notifications.message_type does not exist yet, skipping backfill")
            return
        }

        val updated = jdbc.update(
            "update notifications set message_type = ? where message_type is null",
            MessageType.DEFAULT.name
        )
        if (updated > 0) {
            log.info("Backfilled notifications.message_type for {} rows", updated)
        }

        val nullCount = jdbc.queryForObject(
            "select count(*) from notifications where message_type is null",
            Long::class.java
        ) ?: 0L

        if (nullCount == 0L) {
            trySetDefaultAndNotNull()
        } else {
            log.warn("notifications.message_type still has {} NULL values, skipping NOT NULL constraint", nullCount)
        }
    }

    private fun messageTypeColumnExists(): Boolean {
        val exists = jdbc.queryForObject(
            """
            select exists (
                select 1
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'notifications'
                  and column_name = 'message_type'
            )
            """.trimIndent(),
            Boolean::class.java
        )
        return exists == true
    }
    @Suppress("TooGenericExceptionCaught")
    private fun trySetDefaultAndNotNull() {
        try {
            jdbc.execute(
                "alter table notifications alter column message_type set default '${MessageType.DEFAULT.name}'")
            jdbc.execute(
                "alter table notifications alter column message_type set not null")
            log.info("Set default and NOT NULL constraint for notifications.message_type")
        } catch (ex: Exception) {
            log.warn("Failed to enforce default/NOT NULL for notifications.message_type: {}", ex.message)
        }
    }
}
