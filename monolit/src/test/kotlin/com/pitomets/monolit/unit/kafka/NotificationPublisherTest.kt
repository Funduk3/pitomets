// package com.pitomets.monolit.unit.kafka
//
// import com.pitomets.monolit.kafka.notifications.producer.NotificationPublisher
// import com.pitomets.monolit.model.kafka.NotificationRequestedEvent
// import com.pitomets.monolit.model.kafka.event.Channel
// import com.pitomets.monolit.testContainers.BaseContainers
// import net.datafaker.PhoneNumber
// import org.apache.kafka.clients.consumer.ConsumerConfig
// import org.apache.kafka.clients.consumer.ConsumerRecord
// import org.apache.kafka.common.serialization.StringDeserializer
// import org.assertj.core.api.Assertions.assertThat
// import org.junit.jupiter.api.AfterEach
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.MethodOrderer
// import org.junit.jupiter.api.Order
// import org.junit.jupiter.api.TestMethodOrder
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.context.SpringBootTest
// import org.springframework.kafka.core.DefaultKafkaConsumerFactory
// import org.springframework.kafka.listener.ContainerProperties
// import org.springframework.kafka.listener.KafkaMessageListenerContainer
// import org.springframework.kafka.listener.MessageListener
// import org.springframework.kafka.support.serializer.JsonDeserializer
// import org.springframework.kafka.test.utils.ContainerTestUtils
// import org.springframework.test.context.ActiveProfiles
// import org.testcontainers.junit.jupiter.Testcontainers
// import java.util.concurrent.BlockingQueue
// import java.util.concurrent.LinkedBlockingQueue
// import java.util.concurrent.TimeUnit
// import kotlin.test.Test
//
// @SpringBootTest(
//    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
// )
// @ActiveProfiles("test")
// @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
// @Testcontainers
// class NotificationPublisherTest : BaseContainers() {
//
//    @Autowired
//    private lateinit var notificationPublisher: NotificationPublisher
//
//    private var consumerContainer: KafkaMessageListenerContainer<String, NotificationRequestedEvent>? = null
//    private var records: BlockingQueue<ConsumerRecord<String, NotificationRequestedEvent>>? = null
//
//    @BeforeEach
//    fun setUpConsumer() {
//        records = LinkedBlockingQueue()
//        consumerContainer = createTestContainer(records!!)
//
//        try {
//            consumerContainer?.start()
//            ContainerTestUtils.waitForAssignment(consumerContainer!!, 1)
//
//            Thread.sleep(1000)
//
//            records?.clear()
//        } catch (e: Exception) {
//            consumerContainer?.stop()
//            throw e
//        }
//    }
//
//    @AfterEach
//    fun tearDownConsumer() {
//        try {
//            consumerContainer?.stop()
//            records?.clear()
//        } finally {
//            consumerContainer = null
//            records = null
//        }
//
//        Thread.sleep(500)
//    }
//
//    @Test
//    @Order(1)
//    fun `should publish email notification event to Kafka`() {
//        val email: String = faker.internet().emailAddress()
//        val eventId = 12345L + System.nanoTime() % 10000
//
//        notificationPublisher.publish(
//            NotificationRequestedEvent(
//                eventId = eventId,
//                userId = 100L,
//                channel = Channel.EMAIL,
//                payload = email
//            )
//        )
//
//        val received = records?.poll(10, TimeUnit.SECONDS)
//        assertThat(received).withFailMessage("Не получили сообщение из Kafka").isNotNull
//
//        val event = received?.value()
//        assertThat(event?.eventId).isEqualTo(eventId)
//        assertThat(event?.userId).isEqualTo(100L)
//        assertThat(event?.channel)
//            .withFailMessage("Ожидался канал EMAIL, но получен ${event?.channel}")
//            .isEqualTo(Channel.EMAIL)
//        assertThat(event?.payload).contains(email)
//    }
//
//    @Test
//    @Order(2)
//    fun `should publish SMS notification event to Kafka`() {
//        val phoneNumber: PhoneNumber = faker.phoneNumber()
//        val eventId = 67890L + System.nanoTime() % 10000 // Уникальный ID
//
//        notificationPublisher.publish(
//            NotificationRequestedEvent(
//                eventId = eventId,
//                userId = 200L,
//                channel = Channel.SMS,
//                payload = "$phoneNumber your code is 1234"
//            )
//        )
//
//        val received = records?.poll(10, TimeUnit.SECONDS)
//        assertThat(received).withFailMessage("Не получили сообщение из Kafka").isNotNull
//
//        val event = received?.value()
//        assertThat(event?.eventId).isEqualTo(eventId)
//        assertThat(event?.userId).isEqualTo(200L)
//        assertThat(event?.channel)
//            .withFailMessage("Ожидался канал SMS, но получен ${event?.channel}")
//            .isEqualTo(Channel.SMS)
//        assertThat(event?.payload).contains(phoneNumber.toString())
//        assertThat(event?.payload).contains("1234")
//    }
//
//    @Test
//    @Order(3)
//    fun `should publish push notification event to Kafka`() {
//        val eventId = 11111L + System.nanoTime() % 10000 // Уникальный ID
//
//        notificationPublisher.publish(
//            NotificationRequestedEvent(
//                eventId = eventId,
//                userId = 300L,
//                channel = Channel.PUSH,
//                payload = "You have a new message"
//            )
//        )
//
//        val received = records?.poll(10, TimeUnit.SECONDS)
//        assertThat(received).withFailMessage("Не получили сообщение из Kafka").isNotNull
//
//        val event = received?.value()
//        assertThat(event?.eventId).isEqualTo(eventId)
//        assertThat(event?.userId).isEqualTo(300L)
//        assertThat(event?.channel)
//            .withFailMessage("Ожидался канал PUSH, но получен ${event?.channel}")
//            .isEqualTo(Channel.PUSH)
//        assertThat(event?.payload).contains("You have a new message")
//    }
//
//    private fun createTestContainer(
//        records: BlockingQueue<ConsumerRecord<String, NotificationRequestedEvent>>
//    ): KafkaMessageListenerContainer<String, NotificationRequestedEvent> {
//        val uniqueGroupId = "test-${this.javaClass.simpleName}-${System.currentTimeMillis()}-${System.nanoTime()}"
//
//        val consumerProps = mapOf(
//            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
//            ConsumerConfig.GROUP_ID_CONFIG to uniqueGroupId,
//            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest", // Читаем только новые сообщения
//            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
//            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
//            JsonDeserializer.TRUSTED_PACKAGES to "*",
//            JsonDeserializer.VALUE_DEFAULT_TYPE to NotificationRequestedEvent::class.java.name,
//            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
//            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1,
//            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_uncommitted"
//        )
//
//        val consumerFactory = DefaultKafkaConsumerFactory<String, NotificationRequestedEvent>(consumerProps)
//        val containerProperties = ContainerProperties("notification.send").apply {
//            pollTimeout = 3000
//        }
//
//        val container = KafkaMessageListenerContainer(consumerFactory, containerProperties)
//        container.setupMessageListener(
//            MessageListener { record: ConsumerRecord<String, NotificationRequestedEvent> ->
//                records.add(record)
//            }
//        )
//
//        return container
//    }
// }
