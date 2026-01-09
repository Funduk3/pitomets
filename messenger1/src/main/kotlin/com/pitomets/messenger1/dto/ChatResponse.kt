package com.pitomets.messenger1.dto

import com.pitomets.messenger1.models.ChatEntity
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: Long,
    val user1Id: Long,
    val user2Id: Long,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun from(entity: ChatEntity): ChatResponse {
            return ChatResponse(
                id = entity.id,
                user1Id = entity.user1Id,
                user2Id = entity.user2Id,
                createdAt = entity.createdAt.toString(),
                updatedAt = entity.updatedAt.toString()
            )
        }
    }
}

