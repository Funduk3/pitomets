import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticConfig {

    @Bean
    fun elasticClient(): ElasticsearchClient {
        val restClient = RestClient.builder(
            HttpHost("localhost", Int.MAX_VALUE, "http") // поправить
        ).build()

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper()
        )

        return ElasticsearchClient(transport)
    }
}
