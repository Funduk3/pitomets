package com.pitomets.moderator.infrastructure.client

import com.pitomets.moderator.infrastructure.dto.moderiumAPI.text.ModeriumAnalyzeResponse

interface ModeriumClient {
    fun analyze(
        text: String,
        mode: String,
        withAnimal: Boolean
    ): ModeriumAnalyzeResponse
}
