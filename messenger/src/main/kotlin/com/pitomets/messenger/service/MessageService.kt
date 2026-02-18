package com.pitomets.messenger.service

import com.pitomets.messenger.models.Chat
import com.pitomets.messenger.models.Message
import com.pitomets.messenger.models.MessageEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.ResultSet

class MessageService(
    private val blockService: BlockService
) {
    fun createMessage(chatId: Long, senderId: Long, content: String): MessageEntity {
        return transaction {
            val chatRow = Chat.select { Chat.id eq chatId }.single()
            val user1Id = chatRow[Chat.user1Id]
            val user2Id = chatRow[Chat.user2Id]
            val recipientId = if (senderId == user1Id) user2Id else user1Id

            if (blockService.hasBlockBetween(senderId, recipientId)) {
                throw MessagingBlockedException("Messaging is blocked between users")
            }

            val messageId = Message.insertAndGetId {
                it[Message.chatId] = chatId
                it[Message.senderId] = senderId
                it[Message.content] = content
                it[Message.createdAt] = Clock.System.now()
                it[Message.isRead] = false
            }.value

            // Обновляем updatedAt в чате и инкрементим unreadCount у получателя
            Chat.update({ Chat.id eq chatId }) {
                with(SqlExpressionBuilder) {
                    it[Chat.updatedAt] = Clock.System.now()
                    when (senderId) {
                        user1Id -> {
                            it[Chat.unreadCountUser2] = Chat.unreadCountUser2 + 1
                            it[Chat.lastUnreadMessageIdUser2] = messageId
                        }
                        user2Id -> {
                            it[Chat.unreadCountUser1] = Chat.unreadCountUser1 + 1
                            it[Chat.lastUnreadMessageIdUser1] = messageId
                        }
                    }
                }
            }

            getMessageById(messageId)!!
        }
    }

    fun getMessageById(messageId: Long): MessageEntity? {
        return transaction {
            Message.select { Message.id eq messageId }
                .map { rowToMessage(it) }
                .singleOrNull()
        }
    }

    fun getMessagesByChatId(chatId: Long, limit: Int = 50, offset: Long = 0): List<MessageEntity> {
        return transaction {
            Message.select { Message.chatId eq chatId }
                .orderBy(Message.createdAt to SortOrder.DESC)
                .limit(limit, offset)
                .map { rowToMessage(it) }
                .reversed()
        }
    }

    fun getLastMessageByChatId(chatId: Long): MessageEntity? {
        return transaction {
            Message.select { Message.chatId eq chatId }
                .orderBy(Message.createdAt to SortOrder.DESC)
                .limit(1)
                .map { rowToMessage(it) }
                .singleOrNull()
        }
    }

    fun markMessagesAsRead(chatId: Long, userId: Long) {
        transaction {
            Message.update(
                where = { (Message.chatId eq chatId) and (Message.senderId neq userId) }
            ) {
                it[Message.isRead] = true
            }

            val chatRow = Chat.select { Chat.id eq chatId }.singleOrNull() ?: return@transaction
            val user1Id = chatRow[Chat.user1Id]
            val user2Id = chatRow[Chat.user2Id]

            Chat.update({ Chat.id eq chatId }) {
                when (userId) {
                    user1Id -> {
                        it[Chat.unreadCountUser1] = 0
                        it[Chat.lastUnreadMessageIdUser1] = null
                    }
                    user2Id -> {
                        it[Chat.unreadCountUser2] = 0
                        it[Chat.lastUnreadMessageIdUser2] = null
                    }
                }
            }
        }
    }

    fun getMessagesByIds(ids: Collection<Long>): List<MessageEntity> {
        if (ids.isEmpty()) return emptyList()
        return transaction {
            val entityIds = ids.map { EntityID(it, Message) }
            Message.select { Message.id inList entityIds }
                .map { rowToMessage(it) }
        }
    }

    fun getLastMessagesByChatIds(chatIds: Collection<Long>): Map<Long, MessageEntity> {
        if (chatIds.isEmpty()) return emptyMap()
        return transaction {
            val ids = chatIds.toList()
            val placeholders = ids.joinToString(",") { "?" }
            val sql = """
                SELECT DISTINCT ON (m.chat_id)
                    m.id, m.chat_id, m.sender_id, m.content, m.created_at, m.is_read
                FROM messages m
                WHERE m.chat_id IN ($placeholders)
                ORDER BY m.chat_id, m.created_at DESC
            """.trimIndent()
            val result = linkedMapOf<Long, MessageEntity>()
            val stmt = TransactionManager.current().connection.prepareStatement(sql, false)
            try {
                ids.forEachIndexed { index, id ->
                    stmt.set(index + 1, id)
                }
                val rs = stmt.executeQuery()
                rs.use {
                    while (rs.next()) {
                        val msg = resultSetToMessage(rs)
                        result[msg.chatId] = msg
                    }
                }
            } finally {
                stmt.close()
            }
            result
        }
    }

    /*
     Получить непрочитанные сообщения для пользователя после указанных lastMessageIds.
     @param userId ID пользователя
     @param lastMessageIds Map<chatId, lastMessageId> - последние видимые сообщения для каждого чата
     @return Map<chatId, List<MessageEntity>> - непрочитанные сообщения по чатам (максимум 100 на чат, 1000 всего)
     */
    fun getUnreadMessagesAfter(userId: Long, lastMessageIds: Map<Long, Long>): Map<Long, List<MessageEntity>> {
        if (lastMessageIds.isEmpty()) return emptyMap()

        return transaction {
            val requested = lastMessageIds.toList()
            val valuesPlaceholders = requested.joinToString(",") { "(?, ?)" }
            val sql = """
                WITH last_seen(chat_id, last_message_id) AS (VALUES $valuesPlaceholders)
                SELECT m.id, m.chat_id, m.sender_id, m.content, m.created_at, m.is_read
                FROM messages m
                JOIN last_seen ls ON ls.chat_id = m.chat_id
                JOIN chats c ON c.id = m.chat_id
                WHERE (c.user1_id = ? OR c.user2_id = ?)
                  AND m.sender_id <> ?
                  AND m.is_read = false
                  AND m.id > ls.last_message_id
                ORDER BY m.created_at ASC
                LIMIT ?
            """.trimIndent()
            val result = mutableMapOf<Long, MutableList<MessageEntity>>()
            val stmt = TransactionManager.current().connection.prepareStatement(sql, false)
            try {
                var index = 1
                requested.forEach { (chatId, lastMessageId) ->
                    stmt.set(index++, chatId)
                    stmt.set(index++, lastMessageId)
                }
                stmt.set(index++, userId)
                stmt.set(index++, userId)
                stmt.set(index++, userId)
                stmt.set(index, MAX_UNREAD_MESSAGE_COUNT)

                val rs = stmt.executeQuery()
                rs.use {
                    while (rs.next()) {
                        val msg = resultSetToMessage(rs)
                        val list = result.getOrPut(msg.chatId) { mutableListOf() }
                        if (list.size < MAX_UNREAD_MESSAGE_COUNT_PER_CHAT) {
                            list.add(msg)
                        }
                    }
                }
            } finally {
                stmt.close()
            }
            result
        }
    }

    private fun rowToMessage(row: ResultRow): MessageEntity {
        return MessageEntity(
            id = row[Message.id].value,
            chatId = row[Message.chatId],
            senderId = row[Message.senderId],
            content = row[Message.content],
            createdAt = row[Message.createdAt],
            isRead = row[Message.isRead]
        )
    }

    private fun resultSetToMessage(rs: ResultSet): MessageEntity {
        return MessageEntity(
            id = rs.getLong("id"),
            chatId = rs.getLong("chat_id"),
            senderId = rs.getLong("sender_id"),
            content = rs.getString("content"),
            createdAt = rs.getTimestamp("created_at").toInstant().toKotlinInstant(),
            isRead = rs.getBoolean("is_read")
        )
    }

    companion object {
        const val MAX_UNREAD_MESSAGE_COUNT = 1000
        const val MAX_UNREAD_MESSAGE_COUNT_PER_CHAT = 100
    }
}

class MessagingBlockedException(message: String) : RuntimeException(message)
