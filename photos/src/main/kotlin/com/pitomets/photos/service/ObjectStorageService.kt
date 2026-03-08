package com.pitomets.photos.service

import com.pitomets.photos.exception.ObjectNotFoundException
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ObjectStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket}") private val bucket: String
) {
    fun readObject(objectKey: String): InputStream =
        try {
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectKey)
                    .build()
            )
        } catch (ex: ErrorResponseException) {
            if (ex.errorResponse().code() == "NoSuchKey") {
                throw ObjectNotFoundException("Object '$objectKey' not found")
            }
            throw ex
        }

    fun contentType(objectKey: String): String? =
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectKey)
                    .build()
            ).contentType()
        } catch (ex: ErrorResponseException) {
            if (ex.errorResponse().code() == "NoSuchKey") {
                throw ObjectNotFoundException("Object '$objectKey' not found")
            }
            throw ex
        }
}
