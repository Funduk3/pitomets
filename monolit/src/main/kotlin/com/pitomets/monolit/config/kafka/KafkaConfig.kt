package com.pitomets.monolit.config.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
class KafkaConfig(
    private val properties: KafkaProperties
) {
    companion object {
        private const val INTERVAL = 1000L
        private const val MAX_ATTEMPTS = 3L
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> =
        DefaultKafkaConsumerFactory(
            properties.buildConsumerProperties()
        )

    // ВАЖНО: Добавляем ProducerFactory для общего KafkaTemplate
    @Bean
    fun defaultProducerFactory(): ProducerFactory<String, Any> {
        val configProps = HashMap<String, Any>().apply {
            putAll(properties.buildProducerProperties())
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
            put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false)
        }
        return DefaultKafkaProducerFactory(configProps)
    }

    // ВАЖНО: Общий KafkaTemplate для error handler
    @Bean
    fun kafkaTemplate(defaultProducerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(defaultProducerFactory)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        errorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, Any> =
        ConcurrentKafkaListenerContainerFactory<String, Any>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(errorHandler)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }

    @Bean
    fun kafkaErrorHandler(
        kafkaTemplate: KafkaTemplate<String, Any> // Теперь будет инжектиться правильный bean
    ): DefaultErrorHandler =
        DefaultErrorHandler(
            DeadLetterPublishingRecoverer(kafkaTemplate),
            FixedBackOff(INTERVAL, MAX_ATTEMPTS)
        )
}
