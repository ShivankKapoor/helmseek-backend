package com.shivankkapoor.helmseek_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()

    /** Talks to the aldrop auth service. The API key identifies helmseek's platform and never leaves the backend. */
    @Bean
    fun aldropRestClient(
        @Value("\${app.aldrop.url}") url: String,
        @Value("\${app.aldrop.api-key}") apiKey: String
    ): RestClient = RestClient.builder()
        .baseUrl(url)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        .build()
}
