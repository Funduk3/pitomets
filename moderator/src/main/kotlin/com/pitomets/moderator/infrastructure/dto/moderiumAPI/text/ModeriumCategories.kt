package com.pitomets.moderator.infrastructure.dto.moderiumAPI.text

import com.fasterxml.jackson.annotation.JsonProperty
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.text.ModeriumCategoryMatches
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumToxicity

data class ModeriumCategories(
    val profanity: ModeriumCategoryMatches? = null,
    @JsonProperty("sexual_content")
    val sexualContent: ModeriumCategoryMatches? = null,
    val toxicity: ModeriumToxicity? = null
)