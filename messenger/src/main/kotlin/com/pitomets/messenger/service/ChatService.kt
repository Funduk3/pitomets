package com.pitomets.messenger.service

import com.pitomets.messenger.models.ChatEntity
import com.pitomets.messenger.models.Chats
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ChatService {
    fun createOrGetChat(user1Id: Long, user2Id: Long): ChatEntity {
        return transaction {
            // Проверяем, существует ли уже чат между этими пользователями
            val existingChat = Chats.select {
                ((Chats.user1Id eq user1Id) and (Chats.user2Id eq user2Id)) or
                    ((Chats.user1Id eq user2Id) and (Chats.user2Id eq user1Id))
            }.singleOrNull()

            if (existingChat != null) {
                rowToChat(existingChat)
            } else {
                val chatId = Chats.insertAndGetId {
                    it[Chats.user1Id] = user1Id
                    it[Chats.user2Id] = user2Id
                    it[Chats.createdAt] = Clock.System.now()
                    it[Chats.updatedAt] = Clock.System.now()
                }.value

                getChatById(chatId)!!
            }
        }
    }

    fun getChatById(chatId: Long): ChatEntity? {
        return transaction {
            Chats.select { Chats.id eq chatId }
                .map { rowToChat(it) }
                .singleOrNull()
        }
    }

    fun getUserChats(userId: Long): List<ChatEntity> {
        return transaction {
            Chats.select {
                (Chats.user1Id eq userId) or (Chats.user2Id eq userId)
            }
                .orderBy(Chats.updatedAt to SortOrder.DESC)
                .map { rowToChat(it) }
        }
    }

    fun isUserInChat(chatId: Long, userId: Long): Boolean {
        return transaction {
            val chat = Chats.select { Chats.id eq chatId }.singleOrNull()
            chat != null && (chat[Chats.user1Id] == userId || chat[Chats.user2Id] == userId)
        }
    }

    private fun rowToChat(row: ResultRow): ChatEntity {
        return ChatEntity(
            id = row[Chats.id].value,
            user1Id = row[Chats.user1Id],
            user2Id = row[Chats.user2Id],
            createdAt = row[Chats.createdAt],
            updatedAt = row[Chats.updatedAt],
            unreadCountUser1 = row[Chats.unreadCountUser1],
            unreadCountUser2 = row[Chats.unreadCountUser2],
        )
    }
}
