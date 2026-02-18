package com.pitomets.monolit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncConfig {
    @Bean(name = ["metricsExecutor"])
    fun metricsExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = CORE_POOL_SIZE
        executor.maxPoolSize = MAX_POOL_SIZE
        executor.setQueueCapacity(QUEUE_CAPACITY)
        executor.setThreadNamePrefix("metrics-")
        executor.initialize()
        return executor
    }
    companion object {
        private const val CORE_POOL_SIZE = 2
        private const val MAX_POOL_SIZE = 4
        private const val QUEUE_CAPACITY = 2000
    }
}
