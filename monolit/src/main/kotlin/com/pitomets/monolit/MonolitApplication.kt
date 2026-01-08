package com.pitomets.monolit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MonolitApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<MonolitApplication>(*args)
}
