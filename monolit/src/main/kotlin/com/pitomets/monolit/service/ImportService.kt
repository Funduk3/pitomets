package com.pitomets.monolit.service

import com.pitomets.monolit.model.entity.AnimalType
import com.pitomets.monolit.repository.AnimalTypeRepository
import com.pitomets.monolit.repository.BreedRepository
import com.pitomets.monolit.repository.CitiesRepository
import com.pitomets.monolit.repository.MetroRepository
import com.pitomets.monolit.repository.RegionRepository
import com.pitomets.monolit.utils.data.FileParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImportService(
    private val regionRepository: RegionRepository,
    private val cityRepository: CitiesRepository,
    private val metroRepository: MetroRepository,
    private val animalTypeRepository: AnimalTypeRepository,
    private val breedRepository: BreedRepository,
) {
    @Transactional
    fun importFromFileRegion(path: String) {
        val regions = FileParser.parseRegion(path)
        regionRepository.saveAll(regions)
    }

    @Transactional
    fun importFromFileCity(path: String) {
        val cities = FileParser.parseCity(path)
        cityRepository.saveAll(cities)
    }

    @Transactional
    fun importFromFileMetro(path: String, city: Long) {
        val metro = FileParser.parseMetro(path, city)
        metroRepository.saveAll(metro)
    }

    @Transactional
    fun importFromFileAnimalType(path: String) {
        val animalTypes = FileParser.parseAnimalType(path)
        animalTypeRepository.saveAll(animalTypes)
    }

    @Transactional
    fun importFromFileBreed(path: String, animalType: AnimalType) {
        val breeds = FileParser.parseBreed(path, animalType)
        breedRepository.saveAll(breeds)
    }
}
