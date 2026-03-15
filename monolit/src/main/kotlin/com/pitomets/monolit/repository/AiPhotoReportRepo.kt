package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiPhotoReportRepo : JpaRepository<AiPhotoModerationReport, Long> {
    fun findByPhotoUriAndEntityIdAndEntityType(
        photoUri: String,
        entityId: Long,
        entityType: String
    ): AiPhotoModerationReport?

    fun findByPhotoUriInAndEntityIdAndEntityType(
        photoUris: List<String>,
        entityId: Long,
        entityType: String
    ): List<AiPhotoModerationReport>
    fun findByEntityIdAndEntityType(entityId: Long, entityType: String): List<AiPhotoModerationReport>
}
