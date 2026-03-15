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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class ModerationKafkaConfig(
    private val kafkaProperties: KafkaProperties
) {
    @Bean
    fun moderationConsumerFactory(): ConsumerFactory<String, ModerationProcessedEvent> {
        val props = kafkaProperties.buildConsumerProperties()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
        props[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        props[JsonDeserializer.TRUSTED_PACKAGES] =
            "com.pitomets.monolit.model.kafka.moderation,com.pitomets.moderator.interfaces.messaging.event,com.pitomets.moderator.interfaces.messaging.event.photo"
        props[JsonDeserializer.USE_TYPE_INFO_HEADERS] = true
        props[JsonDeserializer.TYPE_MAPPINGS] =
            "com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent:" +
                "com.pitomets.monolit.model.kafka.moderation.ModerationProcessedEvent," +
                "com.pitomets.moderator.interfaces.messaging.event.photo.ModerationPhotoProcessedEvent:" +
                "com.pitomets.monolit.model.kafka.moderation.ModerationPhotoProcessedEvent"

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
