package com.pitomets.monolit.filter

import com.pitomets.monolit.service.JWTService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JWTFilter(
    private val jwtService: JWTService,
    private val userDetailsService: UserDetailsService,
    @Value("7") private val bearerTokenSize: Int,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring(bearerTokenSize)
            ?.trim()

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
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
        }
        filterChain.doFilter(request, response)
    }
}
