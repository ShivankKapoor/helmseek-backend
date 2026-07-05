package com.shivankkapoor.helmseek_backend.job

import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class QuoteHideResetJobTest {

    private val userRepository = mock<UserRepository>()
    private val job = QuoteHideResetJob(userRepository)

    @Test
    fun `resetHiddenQuotes calls repository`() {
        whenever(userRepository.resetHideQuote()).thenReturn(0)

        job.resetHiddenQuotes()

        verify(userRepository).resetHideQuote()
    }

    @Test
    fun `resetHiddenQuotes with reset rows completes without error`() {
        whenever(userRepository.resetHideQuote()).thenReturn(5)
        job.resetHiddenQuotes()
        verify(userRepository).resetHideQuote()
    }

    @Test
    fun `resetHiddenQuotes with no hidden quotes completes without error`() {
        whenever(userRepository.resetHideQuote()).thenReturn(0)
        job.resetHiddenQuotes()
        verify(userRepository).resetHideQuote()
    }
}
