package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.MetroStationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MetroStationRepo : JpaRepository<MetroStationEntity, Long> {
    fun findTop15ByTitleStartingWithIgnoreCaseOrderByTitle(
        title: String
    ): List<MetroStationEntity>
}
