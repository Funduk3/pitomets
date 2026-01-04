import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticConfig {
    private val log = LoggerFactory.getLogger(ElasticConfig::class.java)

    @Bean
    fun elasticClient(
        @Value("\${elasticsearch.host}") host: String,
        @Value("\${elasticsearch.port}") port: Int,
    ): ElasticsearchClient {
        log.info("Creating Elasticsearch client for host: {} port: {}", host, port)
        val httpHost = HttpHost(host, port, "http")
        val restClient = RestClient.builder(httpHost).build()
        val transport = RestClientTransport(restClient, JacksonJsonpMapper())
        val client = ElasticsearchClient(transport)
        log.info("Elasticsearch client created successfully")
        return client
    }
}
