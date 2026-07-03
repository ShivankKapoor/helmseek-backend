package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.QuoteDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Service
class QuoteService(
    restClientBuilder: RestClient.Builder,
    @Value("\${api.quoteService}") private val quoteServiceURL: String
) {
    companion object {
        private val log = LoggerFactory.getLogger(QuoteService::class.java)
    }

    private val quoteClient = restClientBuilder.baseUrl(quoteServiceURL).build()

    fun getQuote(): QuoteDTO{
        if (quoteServiceURL.isBlank()){
            return QuoteDTO("UNKNOWN", "UNKNOWN")
        }
        return try {
            quoteClient.get().retrieve().body(QuoteDTO::class.java)
                ?: QuoteDTO("UNKNOWN", "UNKNOWN")
        }catch (e: RestClientException) {
            log.error(e.message, e)
            return QuoteDTO("UNKNOWN", "UNKNOWN")
        }
    }
}
