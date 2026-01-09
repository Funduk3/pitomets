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
            val messageId = Messages.insertAndGetId {
                it[Messages.chatId] = chatId
                it[Messages.senderId] = senderId
                it[Messages.content] = content
                it[Messages.createdAt] = Clock.System.now()
                it[Messages.isRead] = false
            }.value

            // Обновляем updatedAt в чате
            Chats.update({ Chats.id eq chatId }) {
                it[Chats.updatedAt] = Clock.System.now()
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

