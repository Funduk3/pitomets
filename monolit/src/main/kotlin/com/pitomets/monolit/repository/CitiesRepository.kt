package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.CityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CitiesRepository : JpaRepository<CityEntity, Long>
