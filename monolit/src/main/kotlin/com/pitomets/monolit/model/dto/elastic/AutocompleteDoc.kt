package com.pitomets.monolit.model.dto.elastic

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AutocompleteDoc(
    @JsonProperty("title") val title: String
)
