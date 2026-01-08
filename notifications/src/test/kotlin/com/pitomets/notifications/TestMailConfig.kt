//package com.pitomets.notifications
//
//import jakarta.mail.internet.MimeMessage
//import org.springframework.boot.test.context.TestConfiguration
//import org.springframework.context.annotation.Bean
//import org.springframework.mail.javamail.JavaMailSender
//import javax.mail.internet.MimeMessage
//
//@TestConfiguration
//class TestMailConfig {
//
//    @Bean
//    fun testJavaMailSender(): JavaMailSender {
//        return object : JavaMailSender {
//            private val sentMessages = mutableListOf<MimeMessage>()
//
//            override fun createMimeMessage(): MimeMessage {
//                return javax.mail.internet.MimeMessage(null as javax.mail.Session?)
//            }
//
//            override fun createMimeMessage(content: ByteArray): MimeMessage {
//                return createMimeMessage()
//            }
//
//            override fun send(mimeMessage: MimeMessage) {
//                sentMessages.add(mimeMessage)
//                println("📧 Тестовый email отправлен: ${mimeMessage.subject}")
//            }
//
//            override fun send(vararg mimeMessages: MimeMessage) {
//                mimeMessages.forEach { send(it) }
//            }
//
//            override fun send(mimeMessage: javax.mail.internet.MimeMessage) {
//                send(mimeMessage as MimeMessage)
//            }
//
//            override fun send(mimeMessagePreparator: org.springframework.mail.javamail.MimeMessagePreparator) {
//                val message = createMimeMessage()
//                mimeMessagePreparator.prepare(message)
//                send(message)
//            }
//
//            override fun send(vararg mimeMessagePreparators: org.springframework.mail.javamail.MimeMessagePreparator) {
//                mimeMessagePreparators.forEach { send(it) }
//            }
//
//            fun getSentMessages() = sentMessages.toList()
//        }
//    }
//}