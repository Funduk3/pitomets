package com.pitomets.monolit.unit.utils

import com.pitomets.monolit.model.entity.converter.StringListJsonConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StringListJsonConverterTest {

    private val converter = StringListJsonConverter()

    @Test
    fun `convertToDatabaseColumn returns null for null attribute`() {
        assertNull(converter.convertToDatabaseColumn(null))
    }

    @Test
    fun `convertToDatabaseColumn serializes list to json`() {
        val json = converter.convertToDatabaseColumn(listOf("a", "b", "c"))

        assertEquals("[\"a\",\"b\",\"c\"]", json)
    }

    @Test
    fun `convertToEntityAttribute returns null for blank data`() {
        assertNull(converter.convertToEntityAttribute(null))
        assertNull(converter.convertToEntityAttribute(""))
        assertNull(converter.convertToEntityAttribute("   "))
    }

    @Test
    fun `convertToEntityAttribute parses json into list`() {
        val result = converter.convertToEntityAttribute("[\"x\",\"y\"]")

        assertEquals(listOf("x", "y"), result)
    }
}
