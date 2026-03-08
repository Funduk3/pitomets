package com.pitomets.photos.controller

import com.pitomets.photos.exception.ApiError
import com.pitomets.photos.exception.ObjectNotFoundException
import io.minio.errors.ErrorResponseException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleObjectNotFound(ex: ObjectNotFoundException): ApiError =
        ApiError(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Object not found"
        )

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: IllegalArgumentException): ApiError =
        ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid request"
        )

    @ExceptionHandler(ErrorResponseException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleMinioError(ex: ErrorResponseException): ApiError =
        ApiError(
            status = HttpStatus.BAD_GATEWAY.value(),
            error = "Bad Gateway",
            message = "Object storage error: ${ex.errorResponse().code()}"
        )
}
