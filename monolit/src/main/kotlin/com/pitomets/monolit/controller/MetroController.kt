package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.response.MetroDto
import com.pitomets.monolit.model.dto.response.MetroLineDto
import com.pitomets.monolit.repository.MetroStationRepo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.collections.map

@RestController
@RequestMapping("/api/metro")
class MetroController(
    private val metroStationRepo: MetroStationRepo,
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam cityId: Long,
    ): List<MetroDto> {
        if (query.length < 2) {
            return emptyList()
        }
        return metroStationRepo
            .findTop5ByTitleStartingWithIgnoreCaseAndLineCityIdOrderByTitle(
                query,
                cityId
            )
            .map {
                MetroDto(
                    it.id,
                    it.title,
                    MetroLineDto(
                        it.line.id,
                        it.line.title,
                        it.line.color
                    )
                )
            }
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long
    ): MetroDto =
        metroStationRepo.findById(id)
            .map {
                MetroDto(
                    it.id,
                    it.title,
                    MetroLineDto(
                        it.line.id,
                        it.line.title,
                        it.line.color
                    )
                )
            }
            .orElseThrow()
}
