package com.pitomets.monolit.config.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties
) {

    companion object {
        private const val INTERVAL = 1000L
        private const val MAX_ATTEMPTS = 3L
    }

    @Bean
    fun notificationStatusConsumerFactory(): ConsumerFactory<String, String> {
        val props = HashMap<String, Any>().apply {
            putAll(kafkaProperties.buildConsumerProperties())
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun notificationStatusListenerFactory(
        notificationStatusConsumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(notificationStatusConsumerFactory)
            setCommonErrorHandler(
                DefaultErrorHandler(
                    FixedBackOff(INTERVAL, MAX_ATTEMPTS)
                )
            )
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }
    }
}
