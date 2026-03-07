package com.pitomets.photos.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig {

    @Bean
    fun minioClient(
        @Value("\${minio.url}") url: String,
        @Value("\${minio.access-key}") accessKey: String,
        @Value("\${minio.secret-key}") secretKey: String
    ): MinioClient =
        MinioClient.builder()
            .endpoint(url)
            .credentials(accessKey, secretKey)
            .build()
}
