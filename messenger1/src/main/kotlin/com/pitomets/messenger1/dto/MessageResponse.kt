package com.pitomets.messenger1.dto

import com.pitomets.messenger1.models.MessageEntity
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val content: String,
    val createdAt: String,
    val isRead: Boolean
) {
    companion object {
        fun from(entity: MessageEntity): MessageResponse {
            return MessageResponse(
                id = entity.id,
                chatId = entity.chatId,
                senderId = entity.senderId,
                content = entity.content,
                createdAt = entity.createdAt.toString(),
                isRead = entity.isRead
            )
        }
    }
}
