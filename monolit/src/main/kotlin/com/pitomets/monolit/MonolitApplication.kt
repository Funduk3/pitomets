package com.pitomets.monolit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MonolitApplication

fun main(args: Array<String>) {
	runApplication<MonolitApplication>(*args)
}
