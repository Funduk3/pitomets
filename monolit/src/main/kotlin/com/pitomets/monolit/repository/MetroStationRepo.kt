package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.MetroStationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MetroStationRepo : JpaRepository<MetroStationEntity, Long> {
    fun findTop5ByTitleStartingWithIgnoreCaseAndLineCityIdOrderByTitle(
        title: String,
        cityId: Long
    ): List<MetroStationEntity>
}
