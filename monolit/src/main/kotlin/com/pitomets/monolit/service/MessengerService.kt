package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.EmptyResponseException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class MessengerService(
    @Value("\${messenger.url}")
    private val messengerUrl: String
) {
    private val restTemplate = RestTemplate()
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("MessengerService proxy base URL: {}", messengerUrl)
    }

    @Suppress("TooGenericExceptionThrown")
    fun proxyRequest(
        userId: Long,
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String> = emptyMap()
    ): Any {
        val url = buildUrl(path, queryParams)
        val headers = HttpHeaders().apply {
            set("X-User-Id", userId.toString())
            contentType = MediaType.APPLICATION_JSON
        }

        val entity = if (body != null) {
            HttpEntity(body, headers)
        } else {
            HttpEntity(headers)
        }

        return when (method) {
            HttpMethod.GET -> restTemplate.exchange(url, HttpMethod.GET, entity, Any::class.java).body
            HttpMethod.POST -> restTemplate.exchange(url, HttpMethod.POST, entity, Any::class.java).body
            HttpMethod.PUT -> restTemplate.exchange(url, HttpMethod.PUT, entity, Any::class.java).body
            HttpMethod.DELETE -> restTemplate.exchange(url, HttpMethod.DELETE, entity, Any::class.java).body
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        } ?: throw EmptyResponseException("Empty response from messenger service")
    }

    private fun buildUrl(path: String, queryParams: Map<String, String>): String {
        val baseUrl = messengerUrl.removeSuffix("/")
        val cleanPath = path.removePrefix("/")
        val url = "$baseUrl/$cleanPath"

        return if (queryParams.isNotEmpty()) {
            val queryString = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$url?$queryString"
        } else {
            url
        }
    }
}
