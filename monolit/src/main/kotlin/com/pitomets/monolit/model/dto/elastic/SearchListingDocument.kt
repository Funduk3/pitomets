package com.pitomets.monolit.model.dto.elastic

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchListingDocument(
    @JsonProperty("id") val id: Long,
    @JsonProperty("title") val title: String,
    @JsonProperty("description") val description: String,
    @JsonProperty("city") val city: Long,
    @JsonProperty("metro") val metro: Long? = null,
    @JsonProperty("price") val price: BigDecimal,
)
