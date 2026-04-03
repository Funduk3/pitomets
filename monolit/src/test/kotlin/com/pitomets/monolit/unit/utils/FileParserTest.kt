package com.pitomets.monolit.unit.utils

import com.pitomets.monolit.model.entity.AnimalType
import com.pitomets.monolit.utils.data.FileParser
import com.pitomets.monolit.utils.data.hasBreed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileParserTest {

    @Test
    fun `hasBreed returns true only for supported animal titles`() {
        assertTrue(hasBreed("Собаки"))
        assertTrue(hasBreed("Кошки"))
        assertFalse(hasBreed("Птицы"))
    }

    @Test
    fun `parseAnimalType reads titles and computes hasBreed`() {
        val result = FileParser.parseAnimalType("data/test/animal_types.txt")

        assertEquals(3, result.size)
        assertEquals("Собаки", result[0].title)
        assertTrue(result[0].hasBreed)
        assertEquals("Кошки", result[1].title)
        assertTrue(result[1].hasBreed)
        assertEquals("Птицы", result[2].title)
        assertFalse(result[2].hasBreed)
    }

    @Test
    fun `parseBreed binds each breed to provided animal type`() {
        val animalType = AnimalType(title = "Собаки", hasBreed = true)

        val result = FileParser.parseBreed("data/test/breeds.txt", animalType)

        assertEquals(2, result.size)
        assertEquals("Лабрадор", result[0].title)
        assertEquals(animalType, result[0].animalType)
        assertEquals("Мейн-кун", result[1].title)
        assertEquals(animalType, result[1].animalType)
    }

    @Test
    fun `parseMetro maps lines and stations with provided city id`() {
        val result = FileParser.parseMetro("data/test/metro.json", cityId = 77L)

        assertEquals(1, result.size)

        val line = result.first()
        assertEquals(10L, line.id)
        assertEquals("Красная", line.title)
        assertEquals("#ff0000", line.color)
        assertEquals(77L, line.cityId)

        assertEquals(2, line.stations.size)
        assertEquals("Станция 1", line.stations[0].title)
        assertEquals(line, line.stations[0].line)
    }
}
