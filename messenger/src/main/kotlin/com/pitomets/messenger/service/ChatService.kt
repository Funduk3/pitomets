package com.pitomets.messenger.service

import com.pitomets.messenger.models.Chat
import com.pitomets.messenger.models.ChatEntity
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ChatService {
    fun createOrGetChat(user1Id: Long, user2Id: Long, listingId: Long, listingTitle: String?): ChatEntity {
        return transaction {
            // Проверяем, существует ли уже чат между этими пользователями
            val existingChat = Chat.select {
                (
                    ((Chat.user1Id eq user1Id) and (Chat.user2Id eq user2Id)) or
                        ((Chat.user1Id eq user2Id) and (Chat.user2Id eq user1Id))
                    ) and (Chat.listingId eq listingId)
            }.singleOrNull()

            if (existingChat != null) {
                rowToChat(existingChat)
            } else {
                val chatId = Chat.insertAndGetId {
                    it[Chat.user1Id] = user1Id
                    it[Chat.user2Id] = user2Id
                    it[Chat.listingId] = listingId
                    it[Chat.listingTitle] = listingTitle
                    it[Chat.createdAt] = Clock.System.now()
                    it[Chat.updatedAt] = Clock.System.now()
                }.value

                getChatById(chatId)!!
            }
        }
    }

    fun getChatById(chatId: Long): ChatEntity? {
        return transaction {
            Chat.select { Chat.id eq chatId }
                .map { rowToChat(it) }
                .singleOrNull()
        }
    }

    fun getUserChats(userId: Long): List<ChatEntity> {
        return transaction {
            Chat.select {
                (Chat.user1Id eq userId) or (Chat.user2Id eq userId)
            }
                .orderBy(Chat.updatedAt to SortOrder.DESC)
                .map { rowToChat(it) }
        }
    }

    fun isUserInChat(chatId: Long, userId: Long): Boolean {
        return transaction {
            val chat = Chat.select { Chat.id eq chatId }.singleOrNull()
            chat != null && (chat[Chat.user1Id] == userId || chat[Chat.user2Id] == userId)
        }
    }

    private fun rowToChat(row: ResultRow): ChatEntity {
        return ChatEntity(
            id = row[Chat.id].value,
            user1Id = row[Chat.user1Id],
            user2Id = row[Chat.user2Id],
            listingId = row[Chat.listingId],
            listingTitle = row[Chat.listingTitle],
            createdAt = row[Chat.createdAt],
            updatedAt = row[Chat.updatedAt],
            unreadCountUser1 = row[Chat.unreadCountUser1],
            unreadCountUser2 = row[Chat.unreadCountUser2],
        )
    }
}
