package com.pitomets.monolit.config

import com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class ModerationKafkaConfig(
    private val kafkaProperties: KafkaProperties
) {
    @Bean
    fun moderationConsumerFactory(): ConsumerFactory<String, ModerationProcessedEvent> {
        val props = kafkaProperties.buildConsumerProperties()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[JsonDeserializer.TRUSTED_PACKAGES] = "com.pitomets.monolit.model.kafka.moderation"
        props[JsonDeserializer.USE_TYPE_INFO_HEADERS] = false
        props[JsonDeserializer.VALUE_DEFAULT_TYPE] = ModerationProcessedEvent::class.java.name

        return DefaultKafkaConsumerFactory(props)
    }

    @Bean(name = ["moderationKafkaListenerContainerFactory"])
    fun moderationKafkaListenerContainerFactory(
        moderationConsumerFactory: ConsumerFactory<String, ModerationProcessedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, ModerationProcessedEvent> =
        ConcurrentKafkaListenerContainerFactory<String, ModerationProcessedEvent>().apply {
            setConsumerFactory(moderationConsumerFactory)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }
}
