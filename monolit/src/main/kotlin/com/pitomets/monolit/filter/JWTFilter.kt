package com.pitomets.monolit.filter

import com.pitomets.monolit.service.JWTService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JWTFilter(
    private val jwtService: JWTService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI.startsWith("/auth/") || request.requestURI.startsWith("/public/")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring(7)
            ?.trim()

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val username = jwtService.extractUsername(token)
                if (!username.isNullOrBlank()) {
                    val userDetails = userDetailsService.loadUserByUsername(username)

                    if (jwtService.validateToken(token, userDetails.username)) {
                        val auth = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = auth

                        log.debug("User '{}' authenticated successfully", username)
                    } else {
                        log.warn("Invalid JWT token for user: {}", username)
                    }
                }
            } catch (ex: Exception) {
                // Не бросаем exception, просто логируем и продолжаем цепочку
                log.warn("JWT authentication failed: {}", ex.message)
            }
        }

        filterChain.doFilter(request, response)
    }
}