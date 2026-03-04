package com.pitomets.moderator.infrastructure.client

interface ModeriumClient {
    fun analyze(
        text: String,
        mode: String,
        withAnimal: Boolean
    ): ModeriumAnalyzeResponse
}
