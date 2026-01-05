package com.pitomets.monolit.controller.controllerAdvice

import com.pitomets.monolit.exceptions.AlreadyException
import com.pitomets.monolit.exceptions.AvatarNotFoundException
import com.pitomets.monolit.exceptions.BadReviewException
import com.pitomets.monolit.exceptions.ErrorResponse
import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.UserAlreadyExistsException
import com.pitomets.monolit.exceptions.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.nio.file.AccessDeniedException

@RestControllerAdvice
class ControllerAdvice {
    private val log = LoggerFactory.getLogger(ControllerAdvice::class.java)

    @ExceptionHandler(UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUserAlreadyExistsException(ex: UserAlreadyExistsException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message
        )
    }

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFoundException(ex: UserNotFoundException): ErrorResponse {
        log.error("UserNotFoundException: {}", ex.message, ex)
        return ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(ex: AccessDeniedException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = ex.message ?: "Access denied"
        )
    }

    @ExceptionHandler(BadReviewException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadReviewException(ex: BadReviewException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message
        )
    }

    @ExceptionHandler(AvatarNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleAvatarNotFoundException(ex: AvatarNotFoundException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message
        )
    }

    @ExceptionHandler(AlreadyException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleAlreadyException(ex: AlreadyException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message
        )
    }

    @ExceptionHandler(ListingNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleListingNotFoundException(ex: ListingNotFoundException): ErrorResponse {
        log.error("ListingNotFoundException: {}", ex.message, ex)
        return ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message
        )
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoSuchElementException(ex: NoSuchElementException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Resource not found"
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): ErrorResponse {
        log.error("Unhandled exception: {}", ex.message, ex)
        return ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred"
        )
    }
}
