package com.pitomets.monolit.controller

import com.pitomets.monolit.components.DbInitRunner.Companion.COLOR_MAPPINGS
import com.pitomets.monolit.model.dto.response.CityDto
import com.pitomets.monolit.repository.CitiesRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cities")
class CityController(
    private val cityRepository: CitiesRepository
) {
    @GetMapping
    fun search(
        @RequestParam query: String
    ): List<CityDto> {
        if (query.length < 2) {
            return emptyList()
        }
        return cityRepository
            .findTop15ByTitleStartingWithIgnoreCaseOrderByTitle(query)
            .map {
                CityDto(
                    it.id,
                    it.title,
                    COLOR_MAPPINGS.containsKey(it.id.toInt())
                )
            }
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long
    ): CityDto =
        cityRepository.findById(id)
            .map {
                CityDto(
                    it.id,
                    it.title,
                    COLOR_MAPPINGS.containsKey(it.id.toInt())
                )
            }
            .orElseThrow()
}
