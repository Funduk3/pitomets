package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Pet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PetsRepo : JpaRepository<Pet, Long>
