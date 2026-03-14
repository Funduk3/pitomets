package com.pitomets.monolit.model.entity.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListJsonConverter : AttributeConverter<List<String>?, String?> {
    private val mapper = jacksonObjectMapper()
    private val typeRef = object : TypeReference<List<String>>() {}

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        if (attribute == null) {
            return null
        }
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        if (dbData.isNullOrBlank()) {
            return null
        }
        return mapper.readValue(dbData, typeRef)
    }
}
