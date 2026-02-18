package com.pitomets.monolit.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Duration
import org.springframework.data.redis.core.ScanOptions
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class ListingMetricsService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Async("metricsExecutor")
    @Suppress("TooGenericExceptionCaught")
    fun recordViewAsync(
        listingId: Long,
        ownerId: Long?,
        viewerId: Long?,
        ip: String?,
        userAgent: String?
    ) {
        try {
            recordView(listingId, ownerId, viewerId, ip, userAgent)
        } catch (ex: Exception) {
            log.warn("Failed to record view for listing {}", listingId, ex)
        }
    }

    @Suppress("ReturnCount")
    fun recordView(
        listingId: Long,
        ownerId: Long?,
        viewerId: Long?,
        ip: String?,
        userAgent: String?
    ): Boolean {
        if (viewerId != null && ownerId != null && viewerId == ownerId) {
            return false
        }

        val actorKey = buildActorKey(viewerId, ip, userAgent) ?: return false
        val dedupKey = viewDedupKey(listingId, actorKey)
        val deltaKey = viewDeltaKey(listingId)
        val result = redisTemplate.execute(
            VIEW_RECORD_SCRIPT,
            listOf(dedupKey, deltaKey),
            VIEW_DEDUP_TTL.seconds.toString(),
            DELTA_TTL.seconds.toString()
        )
        return result == 1L
    }

    fun recordLikeDelta(listingId: Long, delta: Long) {
        val deltaKey = likeDeltaKey(listingId)
        redisTemplate.execute(
            LIKE_DELTA_SCRIPT,
            listOf(deltaKey),
            delta.toString(),
            DELTA_TTL.seconds.toString()
        )
    }

    fun getPendingViewsDelta(listingId: Long): Long =
        redisTemplate.opsForValue().get(viewDeltaKey(listingId))?.toLongOrNull() ?: 0

    fun getPendingLikesDelta(listingId: Long): Long =
        redisTemplate.opsForValue().get(likeDeltaKey(listingId))?.toLongOrNull() ?: 0

    @Transactional
    fun flushDeltas() {
        flushType(
            keyPattern = VIEW_DELTA_PATTERN,
            applyBatch = { batch -> applyViewsBatch(batch) }
        )
        flushType(
            keyPattern = LIKE_DELTA_PATTERN,
            applyBatch = { batch -> applyLikesBatch(batch) }
        )
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private fun flushType(
        keyPattern: String,
        applyBatch: (List<Pair<Long, Long>>) -> Unit
    ) {
        val keys = scanKeys(keyPattern)
        if (keys.isEmpty()) return

        val batch = mutableListOf<Pair<Long, Long>>()
        val keysToDelete = mutableListOf<String>()
        val batchId = UUID.randomUUID().toString()

        keys.forEach { key ->
            val isProcessing = key.contains(":processing:")
            val baseKey = if (isProcessing) key.substringBefore(":processing:") else key
            val listingId = baseKey.substringAfterLast(":").toLongOrNull()
            if (listingId == null) {
                redisTemplate.delete(key)
                return@forEach
            }

            val processingKey = if (isProcessing) key else "$baseKey:processing:$batchId"
            if (!isProcessing) {
                val renamed = redisTemplate.renameIfAbsent(baseKey, processingKey)
                if (renamed != true) return@forEach
            }

            val delta = redisTemplate.opsForValue().get(processingKey)?.toLongOrNull()
            if (delta == null || delta == 0L) {
                redisTemplate.delete(processingKey)
                return@forEach
            }

            batch.add(listingId to delta)
            keysToDelete.add(processingKey)
        }

        if (batch.isEmpty()) return

        try {
            applyBatch(batch)
        } catch (ex: Exception) {
            log.error("Failed to apply batch for pattern {}", keyPattern, ex)
            return
        }

        redisTemplate.delete(keysToDelete)
    }

    private fun scanKeys(pattern: String): List<String> {
        val options = ScanOptions.scanOptions().match(pattern).count(KEYS_COUNT).build()
        return redisTemplate.execute { connection ->
            val result = mutableListOf<String>()
            connection.scan(options).use { cursor ->
                while (cursor.hasNext()) {
                    val raw = cursor.next()
                    result.add(String(raw, StandardCharsets.UTF_8))
                }
            }
            result
        } ?: emptyList()
    }

    private fun applyViewsBatch(batch: List<Pair<Long, Long>>) {
        val sql = buildValuesUpdateSql(
            table = "listing",
            column = "views_count",
            useGreatest = false,
            size = batch.size
        )
        jdbcTemplate.update(sql, *buildParams(batch))
    }

    private fun applyLikesBatch(batch: List<Pair<Long, Long>>) {
        val sql = buildValuesUpdateSql(
            table = "listing",
            column = "likes_count",
            useGreatest = true,
            size = batch.size
        )
        jdbcTemplate.update(sql, *buildParams(batch))
    }

    private fun buildValuesUpdateSql(
        table: String,
        column: String,
        useGreatest: Boolean,
        size: Int
    ): String {
        val values = List(size) { "(?, ?)" }.joinToString(", ")
        val expr = if (useGreatest) {
            "GREATEST($column + v.delta, 0)"
        } else {
            "$column + v.delta"
        }
        return """
            update $table
            set $column = $expr
            from (values $values) as v(id, delta)
            where $table.id = v.id
        """.trimIndent()
    }

    private fun buildParams(batch: List<Pair<Long, Long>>): Array<Any> =
        batch.flatMap { listOf(it.first, it.second) }.toTypedArray()

    @Suppress("ReturnCount")
    private fun buildActorKey(viewerId: Long?, ip: String?, userAgent: String?): String? {
        if (viewerId != null) return "u:$viewerId"
        if (ip.isNullOrBlank()) return null

        val raw = listOfNotNull(ip.trim(), userAgent?.trim()).joinToString("|")
        return "ip:${sha256(raw)}"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun viewDeltaKey(listingId: Long) = "listing:views:delta:$listingId"
    private fun likeDeltaKey(listingId: Long) = "listing:likes:delta:$listingId"
    private fun viewDedupKey(listingId: Long, actorKey: String) =
        "listing:views:dedup:$listingId:$actorKey"

    companion object {
        private val log = LoggerFactory.getLogger(ListingMetricsService::class.java)
        private val VIEW_DEDUP_TTL = Duration.ofMinutes(30)
        private val DELTA_TTL = Duration.ofDays(2)
        private const val VIEW_DELTA_PATTERN = "listing:views:delta:*"
        private const val LIKE_DELTA_PATTERN = "listing:likes:delta:*"
        private const val KEYS_COUNT = 1000L

        private val VIEW_RECORD_SCRIPT = DefaultRedisScript<Long>().apply {
            setResultType(Long::class.java)
            setScriptText(
                """
                if redis.call("SET", KEYS[1], "1", "NX", "EX", ARGV[1]) then
                    redis.call("INCRBY", KEYS[2], 1)
                    redis.call("EXPIRE", KEYS[2], ARGV[2])
                    return 1
                else
                    return 0
                end
                """.trimIndent()
            )
        }

        private val LIKE_DELTA_SCRIPT = DefaultRedisScript<Long>().apply {
            setResultType(Long::class.java)
            setScriptText(
                """
                redis.call("INCRBY", KEYS[1], ARGV[1])
                redis.call("EXPIRE", KEYS[1], ARGV[2])
                return 1
                """.trimIndent()
            )
        }
    }
}
