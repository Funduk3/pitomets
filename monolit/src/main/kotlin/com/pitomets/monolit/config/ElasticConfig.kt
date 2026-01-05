package com.pitomets.monolit.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticConfig {
    @Bean
    @Suppress("SpreadOperator")
    fun elasticClient(
        @Value("\${spring.elasticsearch.uris}") uris: List<String>
    ): ElasticsearchClient {
        val restClient = RestClient.builder(
            *uris.map { HttpHost.create(it) }.toTypedArray()
        ).build()
        val transport = RestClientTransport(restClient, JacksonJsonpMapper())
        return ElasticsearchClient(transport)
    }
}
