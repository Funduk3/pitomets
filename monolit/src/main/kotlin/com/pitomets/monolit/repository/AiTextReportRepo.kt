package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.AiTextModerationReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiTextReportRepo : JpaRepository<AiTextModerationReport, Long> {
    fun findByEntityIdAndEntityType(entityId: Long, entityType: String): AiTextModerationReport?
}
