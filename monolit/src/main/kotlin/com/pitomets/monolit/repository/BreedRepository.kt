package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Breed
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BreedRepository : JpaRepository<Breed, Long> {
    fun findTop5ByAnimalTypeIdAndTitleContainingIgnoreCaseOrderByTitle(
        animalTypeId: Long,
        query: String
    ): List<Breed>
}
