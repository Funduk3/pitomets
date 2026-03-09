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
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:3001",
            "https://pitomets.com",
            "https://www.pitomets.com"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = configMaxAge

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    @Suppress("LongMethod")
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
                    "/api/register",
                    "/login",
                    "/api/login",
                    "/refresh",
                    "/api/refresh",
                    "/confirm",
                    "/api/confirm",
                    "/forgot-password",
                    "/api/forgot-password",
                    "/reset-password",
                    "/api/reset-password",
                    "/search/listings/**",
                    "/search/deleteALL", // todo delete
                    "/actuator/prometheus", // todo put in admin
                    "/seller/reviews/**",
                    "/api/cities/**",
                    "/api/metro/**",
                ).permitAll()
                it.requestMatchers(HttpMethod.POST, "/listings/reviews/**").authenticated()
                it.requestMatchers(HttpMethod.PUT, "/listings/reviews/**").authenticated()
                it.requestMatchers(HttpMethod.DELETE, "/listings/reviews/**").authenticated()
                it.requestMatchers(HttpMethod.GET, "/listings/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/seller/profile/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/animal/types").permitAll()
                it.requestMatchers(HttpMethod.GET, "/seller/{sellerId}/reviews/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/users/photos/avatar/*").permitAll()
                it.requestMatchers("/seller/{sellerProfileId}/reviews/**").authenticated()
                it.requestMatchers("/seller/profile").authenticated() // Создание профиля для всех
                it.requestMatchers("/admin/**").hasRole("ADMIN")
                it.requestMatchers("/listings/**").hasRole("SELLER")
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
