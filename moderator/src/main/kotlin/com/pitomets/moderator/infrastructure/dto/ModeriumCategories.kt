package com.pitomets.moderator.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.pitomets.moderator.infrastructure.client.ModeriumCategoryMatches
import com.pitomets.moderator.infrastructure.client.ModeriumToxicity

data class ModeriumCategories(
    val profanity: ModeriumCategoryMatches? = null,
    @JsonProperty("sexual_content")
    val sexualContent: ModeriumCategoryMatches? = null,
    val toxicity: ModeriumToxicity? = null
)