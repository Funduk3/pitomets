package com.pitomets.monolit.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class ElasticConfig {
    @Bean
    @Suppress("SpreadOperator")
    fun elasticClient(env: Environment): ElasticsearchClient {
        val urisProp = env.getProperty("spring.elasticsearch.uris")
            ?: env.getProperty("ELASTICSEARCH_URI")
            ?: ""
        val hosts: List<HttpHost> = if (urisProp.isNotBlank()) {
            urisProp.split(",").map { HttpHost.create(it.trim()) }
        } else {
            val host = env.getProperty("ELASTICSEARCH_HOST", "localhost")
            val port = env.getProperty("ELASTICSEARCH_PORT", "9200").toInt()
            listOf(HttpHost(host, port, "http"))
        }

        val restClient = RestClient.builder(*hosts.toTypedArray()).build()
        val transport = RestClientTransport(restClient, JacksonJsonpMapper())
        return ElasticsearchClient(transport)
    }
}
