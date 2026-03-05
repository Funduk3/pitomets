package com.pitomets.moderator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ModeratorApplication

fun main(args: Array<String>) {
    runApplication<ModeratorApplication>(*args)
}
