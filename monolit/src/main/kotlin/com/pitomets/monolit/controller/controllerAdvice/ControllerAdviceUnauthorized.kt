package com.pitomets.monolit.controller.controllerAdvice

import com.pitomets.monolit.exceptions.ErrorResponse
import com.pitomets.monolit.exceptions.InvalidCredentialsException
import com.pitomets.monolit.exceptions.authExceptions.AuthenticationException
import com.pitomets.monolit.exceptions.jwtException.JWTException
import com.pitomets.monolit.exceptions.refreshTokenException.RefreshTokenException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ControllerAdviceUnauthorized {
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
