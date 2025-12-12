package com.pitomets.monolit.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.exceptions.ErrorResponse
import com.pitomets.monolit.filter.JWTFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtFilter: JWTFilter,
    private val userDetailsService: UserDetailsService,

    @Value("3600")
    private val configMaxAge: Long,
    @Value("12")
    private val passwordEncoderStrength: Int
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        BCryptPasswordEncoder(passwordEncoderStrength)

    @Bean
    fun authenticationProvider(passwordEncoder: PasswordEncoder): AuthenticationProvider {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = configMaxAge

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val mapper = jacksonObjectMapper()

        http
            .csrf { it.disable() }
            .headers { it.frameOptions { it.disable() } }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/register",
                    "/login",
                    "/refresh",
                    "/search/listings",
                    "/actuator/prometheus", // todo put in admin
                ).permitAll()
                it.requestMatchers(HttpMethod.GET, "/listings/**").permitAll()
                it.requestMatchers("/listings/**").hasRole("SELLER")
                it.requestMatchers("/seller/profile").authenticated() // Создание профиля для всех
                it.requestMatchers("/seller/**").hasRole("SELLER")
                it.anyRequest().authenticated()
            }
            .authenticationProvider(authenticationProvider(passwordEncoder()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                // Обработка когда нет аутентификации
                it.authenticationEntryPoint { _, response, authException ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    val errorResponse = ErrorResponse(
                        status = HttpServletResponse.SC_UNAUTHORIZED,
                        error = "Unauthorized",
                        message = authException.message ?: "Authentication required"
                    )
                    response.writer.write(mapper.writeValueAsString(errorResponse))
                }

                // Обработка когда нет прав доступа
                it.accessDeniedHandler { _, response, accessDeniedException ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    val errorResponse = ErrorResponse(
                        status = HttpServletResponse.SC_FORBIDDEN,
                        error = "Forbidden",
                        message = accessDeniedException.message ?: "Access denied"
                    )
                    response.writer.write(mapper.writeValueAsString(errorResponse))
                }
            }
            .logout { it.disable() }

        return http.build()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager
}
