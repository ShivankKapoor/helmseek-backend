package com.shivankkapoor.helmseek_backend

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HelmseekBackendApplication

fun main(args: Array<String>) {
	val dotenv = dotenv { ignoreIfMissing = true }
	dotenv.entries().forEach { System.setProperty(it.key, it.value) }
	runApplication<HelmseekBackendApplication>(*args)
}
