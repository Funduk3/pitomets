package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.RegionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RegionRepository : JpaRepository<RegionEntity, Long>
