package com.pitomets.messenger1.service

import com.pitomets.messenger1.models.ChatEntity
import com.pitomets.messenger1.models.Chats
import com.pitomets.messenger1.models.MessageEntity
import com.pitomets.messenger1.models.Messages
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock

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
                it[Chats.updatedAt] = Clock.System.now()
                when (senderId) {
                    user1Id -> it[Chats.unreadCountUser2] = Chats.unreadCountUser2 + 1
                    user2Id -> it[Chats.unreadCountUser1] = Chats.unreadCountUser1 + 1
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
}

