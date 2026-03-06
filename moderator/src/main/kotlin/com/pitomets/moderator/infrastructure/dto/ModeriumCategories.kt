package com.pitomets.moderator.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumCategories(
    val profanity: ModeriumCategoryMatches? = null,
    @JsonProperty("sexual_content")
    val sexualContent: ModeriumCategoryMatches? = null,
    val toxicity: ModeriumToxicity? = null
)
