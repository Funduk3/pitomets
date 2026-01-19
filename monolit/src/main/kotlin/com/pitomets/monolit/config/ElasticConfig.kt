package com.pitomets.monolit.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class ElasticConfig {
    @Bean
    fun elasticClient(env: Environment): ElasticsearchClient {
        val serverUrl = env.getProperty("spring.elasticsearch.uris")
        return ElasticsearchClient.of { b ->
            b.host(serverUrl)
        }
    }
}
