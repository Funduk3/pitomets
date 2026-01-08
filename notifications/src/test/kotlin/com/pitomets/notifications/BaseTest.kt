package com.pitomets.notifications

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.web.server.LocalServerPort

abstract class BaseTest {

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    companion object {
        val faker = Faker()
        val mapper = jacksonObjectMapper().apply { findAndRegisterModules() }
    }
}