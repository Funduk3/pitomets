package com.pitomets.notifications.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(kotlinModule())
            .registerModule(JavaTimeModule())
        // Можно добавить другие настройки:
        // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}