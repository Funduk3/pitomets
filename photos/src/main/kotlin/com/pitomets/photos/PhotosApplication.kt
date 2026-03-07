package com.pitomets.photos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PhotosApplication

fun main(args: Array<String>) {
    runApplication<PhotosApplication>(*args)
}
