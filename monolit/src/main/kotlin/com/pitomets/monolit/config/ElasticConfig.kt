package com.pitomets.monolit.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import jakarta.annotation.PostConstruct
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

    @Bean
    fun indexInitializer(client: ElasticsearchClient) = object {
        @PostConstruct
        fun init() {
            val exists = client.indices().exists { it.index("listings") }.value()

            if (!exists) {
                client.indices().create { c ->
                    c.index("listings")
                        .mappings { m ->
                            m.properties("title") { p ->
                                p.text { t -> t.analyzer("russian") }
                            }
                            m.properties("description") { p ->
                                p.text { t -> t.analyzer("russian") }
                            }
                        }
                }
            }
        }
    }
}
