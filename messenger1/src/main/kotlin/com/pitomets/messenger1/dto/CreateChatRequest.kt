package com.pitomets.messenger1.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateChatRequest(
    val userId: Long // ID пользователя из монолита для создания чата
)
