package com.pitomets.notifications

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class NotificationsApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<NotificationsApplication>(*args)
}
