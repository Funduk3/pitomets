package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.MetroLineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MetroRepository : JpaRepository<MetroLineEntity, Long>
