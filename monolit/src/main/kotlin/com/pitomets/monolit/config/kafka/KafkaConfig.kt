package com.pitomets.monolit.config.kafka

import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
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

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        errorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, Any> =
        ConcurrentKafkaListenerContainerFactory<String, Any>().apply {
            setConsumerFactory(consumerFactory) // <-- вместо this.consumerFactory = ...
            setCommonErrorHandler(errorHandler)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }

    @Bean
    fun kafkaErrorHandler(
        template: KafkaTemplate<String, Any>
    ): DefaultErrorHandler =
        DefaultErrorHandler(
            DeadLetterPublishingRecoverer(template),
            FixedBackOff(INTERVAL, MAX_ATTEMPTS) // 3 retry
        )
}
