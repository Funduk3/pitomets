package com.pitomets.monolit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableScheduling
@EnableAsync
class MonolitApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<MonolitApplication>(*args)
}
