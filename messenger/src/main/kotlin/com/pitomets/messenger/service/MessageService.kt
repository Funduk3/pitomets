package com.pitomets.messenger.service

import com.pitomets.messenger.models.Chats
import com.pitomets.messenger.models.MessageEntity
import com.pitomets.messenger.models.Messages
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
            val chatRow = Chats.select { Chats.id eq chatId }.single()
            val user1Id = chatRow[Chats.user1Id]
            val user2Id = chatRow[Chats.user2Id]

            val messageId = Messages.insertAndGetId {
                it[Messages.chatId] = chatId
                it[Messages.senderId] = senderId
                it[Messages.content] = content
                it[Messages.createdAt] = Clock.System.now()
                it[Messages.isRead] = false
            }.value

            // Обновляем updatedAt в чате и инкрементим unreadCount у получателя
            Chats.update({ Chats.id eq chatId }) {
                with(SqlExpressionBuilder) {
                    it[Chats.updatedAt] = Clock.System.now()
                    when (senderId) {
                        user1Id -> it[Chats.unreadCountUser2] = Chats.unreadCountUser2 + 1
                        user2Id -> it[Chats.unreadCountUser1] = Chats.unreadCountUser1 + 1
                    }
                }
            }

            getMessageById(messageId)!!
        }
    }

    fun getMessageById(messageId: Long): MessageEntity? {
        return transaction {
            Messages.select { Messages.id eq messageId }
                .map { rowToMessage(it) }
                .singleOrNull()
        }
    }

    fun getMessagesByChatId(chatId: Long, limit: Int = 50, offset: Long = 0): List<MessageEntity> {
        return transaction {
            Messages.select { Messages.chatId eq chatId }
                .orderBy(Messages.createdAt to SortOrder.DESC)
                .limit(limit, offset)
                .map { rowToMessage(it) }
                .reversed()
        }
    }

    fun getLastMessageByChatId(chatId: Long): MessageEntity? {
        return transaction {
            Messages.select { Messages.chatId eq chatId }
                .orderBy(Messages.createdAt to SortOrder.DESC)
                .limit(1)
                .map { rowToMessage(it) }
                .singleOrNull()
        }
    }

    fun markMessagesAsRead(chatId: Long, userId: Long) {
        transaction {
            Messages.update(
                where = { (Messages.chatId eq chatId) and (Messages.senderId neq userId) }
            ) {
                it[Messages.isRead] = true
            }

            val chatRow = Chats.select { Chats.id eq chatId }.singleOrNull() ?: return@transaction
            val user1Id = chatRow[Chats.user1Id]
            val user2Id = chatRow[Chats.user2Id]

            Chats.update({ Chats.id eq chatId }) {
                when (userId) {
                    user1Id -> it[Chats.unreadCountUser1] = 0
                    user2Id -> it[Chats.unreadCountUser2] = 0
                }
            }
        }
    }

    /**
     * Получить непрочитанные сообщения для пользователя после указанных lastMessageIds.
     * Оптимизированная версия: один запрос вместо N+1.
     * @param userId ID пользователя
     * @param lastMessageIds Map<chatId, lastMessageId> - последние видимые сообщения для каждого чата
     * @return Map<chatId, List<MessageEntity>> - непрочитанные сообщения по чатам (максимум 100 на чат, 1000 всего)
     */
    fun getUnreadMessagesAfter(userId: Long, lastMessageIds: Map<Long, Long>): Map<Long, List<MessageEntity>> {
        if (lastMessageIds.isEmpty()) return emptyMap()

        return transaction {
            // Шаг 1: Получаем все чаты пользователя одним запросом
            val requestedChatIds = lastMessageIds.keys.toList()
            val userChats = Chats.select {
                ((Chats.user1Id eq userId) or (Chats.user2Id eq userId)) and
                    (Chats.id inList requestedChatIds)
            }.associate {
                it[Chats.id].value to Pair(it[Chats.user1Id], it[Chats.user2Id])
            }

            if (userChats.isEmpty()) return@transaction emptyMap()

            val validChatIds = userChats.keys.toList()

            // Шаг 2: Один запрос для всех непрочитанных сообщений из валидных чатов
            // Лимит 1000 сообщений для защиты от перегрузки
            val allUnread = Messages.select {
                (Messages.chatId inList validChatIds) and
                    (Messages.senderId neq userId) and
                    (Messages.isRead eq false)
            }
                .orderBy(Messages.createdAt to SortOrder.ASC)
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
            id = row[Messages.id].value,
            chatId = row[Messages.chatId],
            senderId = row[Messages.senderId],
            content = row[Messages.content],
            createdAt = row[Messages.createdAt],
            isRead = row[Messages.isRead]
        )
    }

    companion object {
        const val MAX_UNREAD_MESSAGE_COUNT = 1000
        const val MAX_UNREAD_MESSAGE_COUNT_PER_CHAT = 100
    }
}
