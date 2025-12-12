package com.pitomets.monolit.metrics

import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class MetricsInterceptor(private val meterRegistry: MeterRegistry) : HandlerInterceptor {
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) =
        meterRegistry.counter(
            "http_requests_total",
            "path",
            request.servletPath
        ).increment()
}
