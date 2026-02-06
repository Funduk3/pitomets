package com.pitomets.notifications

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = LoggerFactory.getLogger(NotificationsApplication::class.java)

@SpringBootApplication
@EnableScheduling
class NotificationsApplication {

    @Bean
    fun buildInfoLogger(
        buildPropertiesProvider: ObjectProvider<BuildProperties>,
        gitPropertiesProvider: ObjectProvider<GitProperties>
    ) = ApplicationRunner {
        val buildProps = buildPropertiesProvider.ifAvailable
        if (buildProps != null) {
            logger.info(
                "Build info: name={} version={} time={}",
                buildProps.name,
                buildProps.version,
                buildProps.time
            )
        } else {
            logger.info("Build info: not available (build-info.properties not found)")
        }

        val gitProps = gitPropertiesProvider.ifAvailable
        if (gitProps != null) {
            logger.info(
                "Git info: branch={} commit={}",
                gitProps.branch,
                gitProps.get("commit.id") ?: gitProps.shortCommitId
            )
        } else {
            logger.info("Git info: not available (git.properties not found)")
        }
    }
}

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<NotificationsApplication>(*args)
}
