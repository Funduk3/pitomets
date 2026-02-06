package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.response.AnimalTypeDto
import com.pitomets.monolit.model.dto.response.BreedDto
import com.pitomets.monolit.repository.AnimalTypeRepository
import com.pitomets.monolit.repository.BreedRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/animal")
class AnimalController(
    private val breedRepository: BreedRepository,
    private val animalTypeRepository: AnimalTypeRepository,
) {
    @GetMapping("/types")
    fun getAllTypes(): List<AnimalTypeDto> {
        return animalTypeRepository
            .findAll()
            .map {
                AnimalTypeDto(
                    requireNotNull(it.id),
                    it.title,
                    it.hasBreed,
                )
            }
    }

    @GetMapping("/breed/search")
    fun search(
        @RequestParam query: String,
        @RequestParam animalType: Long,
    ): List<BreedDto> {
        if (query.length < 2) {
            return emptyList()
        }
        return breedRepository
            .findTop5ByAnimalTypeIdAndTitleContainingIgnoreCaseOrderByTitle(
                animalType,
                query
            )
            .map {
                BreedDto(
                    requireNotNull(it.id),
                    it.title,
                )
            }
    }
}
