package com.pitomets.monolit.controller

import com.pitomets.monolit.service.MinioService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
class ObjectController(
    private val minioService: MinioService
) {

    @GetMapping("/objects/**")
    fun getObject(request: HttpServletRequest): ResponseEntity<InputStreamResource> {
        val objectKey = extractObjectKey(request.requestURI)
        val stream = minioService.download(objectKey)
        val contentType = minioService.contentType(objectKey) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(InputStreamResource(stream))
    }

    private fun extractObjectKey(requestUri: String): String {
        val prefix = "/objects/"
        require(requestUri.startsWith(prefix)) { "Object key is required" }
        val encoded = requestUri.removePrefix(prefix)
        require(encoded.isNotBlank()) { "Object key is required" }
        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8)
        require(decoded.isNotBlank()) { "Object key is required" }
        return decoded
    }
}
