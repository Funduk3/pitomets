package com.pitomets.messenger.dto

import com.pitomets.messenger.models.ChatEntity
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: Long,
    val user1Id: Long,
    val user2Id: Long,
    val listingId: Long? = null,
    val listingTitle: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val unreadCount: Int = 0,
    val lastMessage: MessageResponse? = null
) {
    companion object {
        fun from(
            entity: ChatEntity,
            unreadCount: Int = 0,
            lastMessage: MessageResponse? = null
        ): ChatResponse {
            return ChatResponse(
                id = entity.id,
                user1Id = entity.user1Id,
                user2Id = entity.user2Id,
                listingId = entity.listingId,
                listingTitle = entity.listingTitle,
                createdAt = entity.createdAt.toString(),
                updatedAt = entity.updatedAt.toString(),
                unreadCount = unreadCount,
                lastMessage = lastMessage
            )
        }
    }
}
