package com.pitomets.messenger.service

import com.pitomets.messenger.models.Chat
import com.pitomets.messenger.models.Message
import com.pitomets.messenger.models.MessageEntity
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class MessageService {
    fun createMessage(chatId: Long, senderId: Long, content: String): MessageEntity {
        return transaction {
            val chatRow = Chat.select { Chat.id eq chatId }.single()
            val user1Id = chatRow[Chat.user1Id]
            val user2Id = chatRow[Chat.user2Id]

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
                        user1Id -> it[Chat.unreadCountUser2] = Chat.unreadCountUser2 + 1
                        user2Id -> it[Chat.unreadCountUser1] = Chat.unreadCountUser1 + 1
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
                    user1Id -> it[Chat.unreadCountUser1] = 0
                    user2Id -> it[Chat.unreadCountUser2] = 0
                }
            }
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
            // Шаг 1: Получаем все чаты пользователя одним запросом
            val requestedChatIds = lastMessageIds.keys.toList()
            val userChats = Chat.select {
                ((Chat.user1Id eq userId) or (Chat.user2Id eq userId)) and
                    (Chat.id inList requestedChatIds)
            }.associate {
                it[Chat.id].value to Pair(it[Chat.user1Id], it[Chat.user2Id])
            }

            if (userChats.isEmpty()) return@transaction emptyMap()

            val validChatIds = userChats.keys.toList()

            // Шаг 2: Один запрос для всех непрочитанных сообщений из валидных чатов
            // Лимит 1000 сообщений для защиты от перегрузки
            val allUnread = Message.select {
                (Message.chatId inList validChatIds) and
                    (Message.senderId neq userId) and
                    (Message.isRead eq false)
            }
                .orderBy(Message.createdAt to SortOrder.ASC)
                .limit(MAX_UNREAD_MESSAGE_COUNT)
                .map { rowToMessage(it) }

            // Шаг 3: Фильтруем по lastMessageId и группируем по чатам в памяти
            val result = mutableMapOf<Long, MutableList<MessageEntity>>()

            for (msg in allUnread) {
                val lastMsgId = lastMessageIds[msg.chatId] ?: continue
                if (msg.id > lastMsgId) {
                    result.getOrPut(msg.chatId) { mutableListOf() }.add(msg)
                }
            }
            // Ограничиваем количество сообщений на чат (максимум 100 на чат)
            result.mapValues { it.value.take(MAX_UNREAD_MESSAGE_COUNT_PER_CHAT) }
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

    companion object {
        const val MAX_UNREAD_MESSAGE_COUNT = 1000
        const val MAX_UNREAD_MESSAGE_COUNT_PER_CHAT = 100
    }
}
