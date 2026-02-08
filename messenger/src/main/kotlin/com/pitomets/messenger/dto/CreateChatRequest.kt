package com.pitomets.messenger.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateChatRequest(
    val userId: Long, // ID пользователя из монолита для создания чата
    val listingId: Long? = null,
    val listingTitle: String? = null
)
