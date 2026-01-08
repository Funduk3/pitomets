package com.pitomets.monolit.config

import com.pitomets.monolit.metrics.MetricsInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// todo проверить нужность этого класса
@Configuration
class WebConfig(
    private val metricsInterceptor: MetricsInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(metricsInterceptor)
    }
}
