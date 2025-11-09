package com.pitomets.monolit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun authenticationProvider(userDetailsService: UserDetailsService): AuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(BCryptPasswordEncoder(12)) }


    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager
        = config.authenticationManager

}