import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class ElasticConfig(
    @Value("\${elasticsearch.host}") private val host: String,
    @Value("\${elasticsearch.port}") private val port: Int,
) {
    private val log = LoggerFactory.getLogger(ElasticConfig::class.java)

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val SOCKET_TIMEOUT_MS = 60000
        private const val CONNECTION_REQUEST_TIMEOUT_MS = 10000
        private const val MAX_CONNECTIONS_TOTAL = 100
        private const val MAX_CONNECTIONS_PER_ROUTE = 100
    }

    @Bean
    @Lazy
    fun elasticClient(): ElasticsearchClient {
        log.info("Creating Elasticsearch client for host: {} port: {}", host, port)

        val httpHost = HttpHost(host, port, "http")
        log.info("Elasticsearch HttpHost: {}", httpHost)

        val restClient = RestClient.builder(httpHost)
            .setRequestConfigCallback { requestConfigBuilder: RequestConfig.Builder ->
                requestConfigBuilder
                    .setConnectTimeout(CONNECT_TIMEOUT_MS)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            }
            .setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder
                    .setMaxConnTotal(MAX_CONNECTIONS_TOTAL)
                    .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
            }
            .build()

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper()
        )

        log.info("Elasticsearch client created successfully")
        return ElasticsearchClient(transport)
    }
}
