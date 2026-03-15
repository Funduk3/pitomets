package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiPhotoReportRepo : JpaRepository<AiPhotoModerationReport, Long> {
    fun findByPhotoUri(photoUri: String): AiPhotoModerationReport?
    fun findByPhotoUriIn(photoUris: List<String>): List<AiPhotoModerationReport>
    fun findByEntityIdAndEntityType(entityId: Long, entityType: String): List<AiPhotoModerationReport>
}
