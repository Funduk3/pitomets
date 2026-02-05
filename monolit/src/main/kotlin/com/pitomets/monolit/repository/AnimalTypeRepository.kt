package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.AnimalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimalTypeRepository : JpaRepository<AnimalType, Long> {
    fun findByTitle(name: String): AnimalType?
}
