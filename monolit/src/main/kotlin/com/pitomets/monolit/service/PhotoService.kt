package com.pitomets.monolit.service

import org.springframework.web.multipart.MultipartFile

open class PhotoService {

    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
        const val MEGABYTE = 1024 * 1014L
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    protected fun validateImage(file: MultipartFile) {
        require(!file.isEmpty) { "File is empty" }

        require(file.contentType in ALLOWED_CONTENT_TYPES) {
            "Unsupported content type: ${file.contentType}. Allowed: ${ALLOWED_CONTENT_TYPES.joinToString()}"
        }

        require(file.size <= MAX_FILE_SIZE) {
            "File is too large. Maximum size: ${MAX_FILE_SIZE / MEGABYTE}MB"
        }
    }

    protected fun extractExtension(filename: String?): String {
        return filename?.substringAfterLast('.', "jpg") ?: "jpg"
    }
}
