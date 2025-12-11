package com.pitomets.monolit.config

import org.apache.catalina.connector.Connector
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class TomcatVirtualThreadConfig {
    @Bean
    fun tomcatCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory?> {
        return WebServerFactoryCustomizer { factory: TomcatServletWebServerFactory? ->
            factory!!.addConnectorCustomizers(
                { connector: Connector? ->
                    connector!!.getProtocolHandler().executor = Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().factory()
                    )
                }
            )
        }
    }
}
