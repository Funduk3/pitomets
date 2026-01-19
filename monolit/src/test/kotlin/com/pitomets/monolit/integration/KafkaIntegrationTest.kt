package com.pitomets.monolit.integration

import com.pitomets.monolit.kafka.notifications.producer.NotificationPublisher
import com.pitomets.monolit.model.kafka.NotificationRequestedEvent
import com.pitomets.monolit.model.kafka.event.Channel
import com.pitomets.monolit.testContainers.BaseContainers
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.kafka.consumer.auto-offset-reset=earliest"
    ]
)
@ActiveProfiles("test")
@Testcontainers
class KafkaIntegrationTest : BaseContainers() {

    @Autowired
    private lateinit var notificationPublisher: NotificationPublisher

    private lateinit var consumerContainer: KafkaMessageListenerContainer<String, NotificationRequestedEvent>
    private lateinit var publishedEvents: BlockingQueue<ConsumerRecord<String, NotificationRequestedEvent>>

    @BeforeEach
    fun setUpConsumer() {
        publishedEvents = LinkedBlockingQueue()
        consumerContainer = createNotificationRequestConsumer(publishedEvents)
        consumerContainer.start()
        ContainerTestUtils.waitForAssignment(consumerContainer, 1)

        Thread.sleep(500)

        publishedEvents.clear()
    }

    @AfterEach
    fun tearDownConsumer() {
        if (::consumerContainer.isInitialized) {
            consumerContainer.stop()
        }
        publishedEvents.clear()
    }

    @Test
    fun `full cycle - publish notification request and verify it in Kafka`() {
        // When - публикуем событие
        notificationPublisher.publish(
            NotificationRequestedEvent(
                eventId = 99999L,
                userId = 555L,
                payload = "integration@test.com",
                channel = Channel.EMAIL
            )
        )

        // Then - проверяем что событие попало в Kafka
        val publishedEvent = publishedEvents.poll(5, TimeUnit.SECONDS)
        assertThat(publishedEvent).isNotNull
        assertThat(publishedEvent?.value()?.eventId).isEqualTo(99999L)
        assertThat(publishedEvent?.value()?.userId).isEqualTo(555L)
        assertThat(publishedEvent?.value()?.channel).isEqualTo(Channel.EMAIL)
        assertThat(publishedEvent?.value()?.payload).contains("integration@test.com")
    }

    @Test
    fun `should handle multiple notifications in sequence`() {
        // When - отправляем несколько уведомлений
        notificationPublisher.publish(
            NotificationRequestedEvent(
                eventId = 1001L,
                userId = 100L,
                channel = Channel.EMAIL,
                payload = "19292"
            )
        )

        notificationPublisher.publish(
            NotificationRequestedEvent(
                eventId = 1002L,
                userId = 200L,
                payload = "+7 909 999 99 99",
                channel = Channel.SMS
            )
        )

        notificationPublisher.publish(
            NotificationRequestedEvent(
                eventId = 1003L,
                userId = 300L,
                channel = Channel.PUSH,
                payload = "bla bla bla",
            )
        )

        // Then - проверяем что все события обработаны
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(publishedEvents.size).isGreaterThanOrEqualTo(3)
        }

        val events = mutableListOf<ConsumerRecord<String, NotificationRequestedEvent>>()
        publishedEvents.drainTo(events)

        assertThat(events).hasSize(3)
        assertThat(events.map { it.value().eventId }).containsExactlyInAnyOrder(1001L, 1002L, 1003L)
        assertThat(events.map { it.value().channel }).containsExactlyInAnyOrder(
            Channel.EMAIL,
            Channel.SMS,
            Channel.PUSH
        )
    }

    @Test
    fun `should handle concurrent notifications`() {
        // When - отправляем множество уведомлений параллельно
        val eventIds = (1L..10L).toList()
        eventIds.forEach { eventId ->
            notificationPublisher.publish(
                NotificationRequestedEvent(
                    eventId = eventId,
                    userId = eventId * 100,
                    channel = Channel.EMAIL,
                    payload = "bla bla bla",
                )
            )
        }

        // Then - проверяем что все события доставлены
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            assertThat(publishedEvents.size).isEqualTo(10)
        }

        val events = mutableListOf<ConsumerRecord<String, NotificationRequestedEvent>>()
        publishedEvents.drainTo(events)

        val receivedEventIds = events.map { it.value().eventId }
        assertThat(receivedEventIds).containsExactlyInAnyOrderElementsOf(eventIds)
    }

    private fun createNotificationRequestConsumer(
        records: BlockingQueue<ConsumerRecord<String, NotificationRequestedEvent>>
    ): KafkaMessageListenerContainer<String, NotificationRequestedEvent> {
        // ВАЖНО: Уникальная группа для каждого теста
        val uniqueGroupId = "test-group-${System.currentTimeMillis()}-${System.nanoTime()}"

        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to BaseContainers.kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to uniqueGroupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest", // Читаем только новые сообщения
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            JsonDeserializer.VALUE_DEFAULT_TYPE to NotificationRequestedEvent::class.java.name,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false // Отключаем auto-commit для тестов
        )

        val consumerFactory = DefaultKafkaConsumerFactory<String, NotificationRequestedEvent>(consumerProps)
        val containerProperties = ContainerProperties("notification.send")

        val container = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        container.setupMessageListener(
            MessageListener { record: ConsumerRecord<String, NotificationRequestedEvent> ->
                records.add(record)
            }
        )

        return container
    }
}
