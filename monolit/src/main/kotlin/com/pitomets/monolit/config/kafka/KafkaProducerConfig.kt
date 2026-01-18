package com.pitomets.monolit.config.kafka

import com.pitomets.monolit.model.kafka.NotificationRequestedEvent
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig(
    private val kafkaProperties: KafkaProperties
) {

    @Bean
    fun notificationProducerFactory(): ProducerFactory<String, NotificationRequestedEvent> {
        val configProps = HashMap<String, Any>().apply {
            putAll(kafkaProperties.buildProducerProperties())
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
            put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false)
        }
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun notificationKafkaTemplate(
        notificationProducerFactory: ProducerFactory<String, NotificationRequestedEvent>
    ): KafkaTemplate<String, NotificationRequestedEvent> {
        return KafkaTemplate(notificationProducerFactory)
    }
}
