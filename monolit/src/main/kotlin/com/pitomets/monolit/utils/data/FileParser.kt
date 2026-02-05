package com.pitomets.monolit.utils.data

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.model.dto.utils.ApiResponseDto
import com.pitomets.monolit.model.dto.utils.CityDto
import com.pitomets.monolit.model.dto.utils.MetroLineDto
import com.pitomets.monolit.model.dto.utils.RegionDto
import com.pitomets.monolit.model.entity.AnimalType
import com.pitomets.monolit.model.entity.Breed
import com.pitomets.monolit.model.entity.CityEntity
import com.pitomets.monolit.model.entity.MetroLineEntity
import com.pitomets.monolit.model.entity.MetroStationEntity
import com.pitomets.monolit.model.entity.RegionEntity
import java.nio.charset.StandardCharsets

fun readJsonFromFile(path: String): String {
    val resource = requireNotNull(
        object {}
            .javaClass.classLoader.getResource(path)
    )

    return resource.readText(StandardCharsets.UTF_8)
}

fun CityDto.toEntity() = CityEntity(
    id = id,
    regionId = idRegion,
    idCountry = idCountry,
    title = title,
    titleEng = titleEng,
)

fun RegionDto.toEntity() = RegionEntity(
    id = id,
    countryId = idCountry,
    title = title
)

fun MetroLineDto.toEntity(cityId: Long): MetroLineEntity {
    val line = MetroLineEntity(
        id = id,
        title = title,
        color = color,
        cityId = cityId
    )

    stations.forEach {
        line.stations.add(
            MetroStationEntity(
                id = it.id,
                title = it.title,
                line = line
            )
        )
    }

    return line
}

fun hasBreed(title: String): Boolean {
    return title == "Собаки" || title == "Кошки"
}

object FileParser {
    // todo use 1 mapper everywhere
    private val mapper = jacksonObjectMapper()

    private fun readTxtFromResources(path: String): List<String> {
        val resource = requireNotNull(
            object {}.javaClass.classLoader.getResource(path)
        ) { "Resource not found: $path" }

        return resource.readText(StandardCharsets.UTF_8)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun parseRegion(filePath: String): List<RegionEntity> {
        val json = readJsonFromFile(filePath)
        val response = mapper.readValue(
            json,
            object : TypeReference<ApiResponseDto<RegionDto>>() {}
        )
        return response.objects.map { it.toEntity() }
    }

    fun parseCity(filePath: String): List<CityEntity> {
        val json = readJsonFromFile(filePath)
        val response = mapper.readValue(
            json,
            object : TypeReference<ApiResponseDto<CityDto>>() {}
        )
        return response.objects.map { it.toEntity() }
    }

    fun parseMetro(path: String, cityId: Long): List<MetroLineEntity> {
        val json = readJsonFromFile(path)

        val dtoList = mapper.readValue(
            json,
            object : TypeReference<List<MetroLineDto>>() {}
        )

        return dtoList.map { it.toEntity(cityId) }
    }

    fun parseAnimalType(path: String): List<AnimalType> {
        return readTxtFromResources(path)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { title ->
                AnimalType(
                    title = title,
                    hasBreed = hasBreed(title),
                )
            }
    }

    fun parseBreed(path: String, animalType: AnimalType): List<Breed> {
        return readTxtFromResources(path)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { title ->
                Breed(
                    title = title,
                    animalType = animalType,
                )
            }
    }
}
