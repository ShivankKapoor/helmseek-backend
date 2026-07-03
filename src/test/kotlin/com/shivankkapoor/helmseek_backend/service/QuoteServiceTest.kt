package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.QuoteDTO
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class QuoteServiceTest {

    @Test
    fun `blank quote service URL returns UNKNOWN`() {
        val quoteService = QuoteService(RestClient.builder(), "")

        assertEquals(QuoteDTO("UNKNOWN", "UNKNOWN"), quoteService.getQuote())
    }

    @Test
    fun `successful response is parsed into QuoteDTO`() {
        val url = "http://quote-service.test/quote"
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(url))
            .andRespond(
                withSuccess(
                    """{"quote":"Stay hungry, stay foolish.","author":"Steve Jobs"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        val quoteService = QuoteService(builder, url)

        assertEquals(QuoteDTO("Stay hungry, stay foolish.", "Steve Jobs"), quoteService.getQuote())
        server.verify()
    }

    @Test
    fun `server error falls back to UNKNOWN`() {
        val url = "http://quote-service.test/quote"
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(url)).andRespond(withServerError())

        val quoteService = QuoteService(builder, url)

        assertEquals(QuoteDTO("UNKNOWN", "UNKNOWN"), quoteService.getQuote())
        server.verify()
    }
}
