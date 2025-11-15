package com.pitomets.monolit.controller.controllerAdvice

import com.pitomets.monolit.exceptions.ErrorResponse
import com.pitomets.monolit.exceptions.InvalidCredentialsException
import com.pitomets.monolit.exceptions.jwtException.JWTException
import com.pitomets.monolit.exceptions.refreshTokenException.RefreshTokenException
import com.pitomets.monolit.exceptions.UserAlreadyExistsException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.authExceptions.AuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ControllerAdvice {

    @ExceptionHandler(InvalidCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidCredentialsException(ex: InvalidCredentialsException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleAuthenticationException(ex: AuthenticationException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message
        )
    }

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

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred"
        )
    }

    @ExceptionHandler(JWTException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleJWTException(ex: JWTException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message
        )
    }

    @ExceptionHandler(RefreshTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleRefreshTokenException(ex: RefreshTokenException): ErrorResponse {
        return ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message
        )
    }
}