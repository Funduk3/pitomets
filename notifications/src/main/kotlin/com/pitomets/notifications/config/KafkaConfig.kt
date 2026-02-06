package com.pitomets.notifications.config

import org.apache.kafka.common.TopicPartition
import com.fasterxml.jackson.core.JsonProcessingException
import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.DeserializationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
class KafkaConfig(
    private val properties: KafkaProperties
) {
    companion object {
        // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: увеличили с 1000L до 10000L (10 секунд)
        private const val INTERVAL = 10000L  // 10 секунд между ретраями
        private const val MAX_ATTEMPTS = 2L  // 3 попытки всего (первая + 2 ретрая)
    }

    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

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
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(errorHandler)
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD

            logger.info("Kafka Consumer configured with 10 second retry interval")
        }

    @Bean
    fun kafkaErrorHandler(
        template: KafkaTemplate<String, Any>
    ): DefaultErrorHandler {
        logger.info("Creating ErrorHandler: interval={}ms, maxAttempts={}", INTERVAL, MAX_ATTEMPTS)

        val recoverer = DeadLetterPublishingRecoverer(template) { record, ex ->
            logger.error(
                "Publishing to DLT topic={} partition={} offset={} key={} due to {}: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                ex.javaClass.simpleName,
                ex.message,
                ex
            )
            TopicPartition("${record.topic()}.DLT", record.partition())
        }

        return DefaultErrorHandler(
            recoverer,
            FixedBackOff(INTERVAL, MAX_ATTEMPTS)
        ).apply {
            // Не ретраим невалидные данные
            addNotRetryableExceptions(
                IllegalArgumentException::class.java,
                DeserializationException::class.java,
                JsonProcessingException::class.java,
                DataIntegrityViolationException::class.java
            )
        }
    }

    @Bean
    fun notificationSendDltTopic() =
        TopicBuilder.name("notification.send.DLT")
            .partitions(1)
            .replicas(1)
            .build()
}
