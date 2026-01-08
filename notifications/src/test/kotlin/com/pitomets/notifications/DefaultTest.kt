package com.pitomets.notifications

import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.use

class DefaultTest {
    @Test
    fun `docker is available`() {
        PostgreSQLContainer<Nothing>("postgres:15-alpine").use { container ->
            container.start()
            assertTrue(container.isRunning)
            println("✅ Successfully started PostgreSQL container")
        }
    }

    @Test
    fun `diagnose docker`() {
        println("=== Docker Environment Diagnostics ===")

        // Check environment variables
        println("DOCKER_HOST: ${System.getenv("DOCKER_HOST")}")
        println("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE: ${System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE")}")

        // Try to get Docker client
        try {
            val dockerClient = DockerClientFactory.instance().client()
            val info = dockerClient.infoCmd().exec()
            println("✅ Docker connected!")
            println("Docker version: ${info.serverVersion}")
            println("OS Type: ${info.osType}")
            println("Architecture: ${info.architecture}")
            println("Total Memory: ${info.memTotal?.div(1024)?.div(1024)} MB")
            println("CPUs: ${info.ncpu}")
        } catch (e: Exception) {
            println("❌ Docker connection failed: ${e.message}")
            e.printStackTrace()
        }
    }
}