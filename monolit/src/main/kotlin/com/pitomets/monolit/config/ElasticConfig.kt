import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticConfig(
    @Value("\${elasticsearch.host}") private val host: String,
    @Value("\${elasticsearch.port}") private val port: Int,
) {

    @Bean
    fun elasticClient(): ElasticsearchClient {
        val restClient = RestClient.builder(
            HttpHost(host, port, "http")
        ).build()

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper()
        )

        return ElasticsearchClient(transport)
    }
}
