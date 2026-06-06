package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.InteractionLog
import com.shivankkapoor.helmseek_backend.repository.InteractionLogRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class InteractionServiceTest {

    private val interactionLogRepository = mock<InteractionLogRepository>()
    private val interactionService = InteractionService(interactionLogRepository)

    private val userId = UUID.randomUUID()
    private val ip = "127.0.0.1"

    @Test
    fun `recordAuthFailed saves log with correct action and ip`() {
        interactionService.recordAuthFailed(ip)

        verify(interactionLogRepository).save(argThat { action == "AUTH FAILED" && this.ip == ip && user == null })
    }

    @Test
    fun `recordAuthSuccess saves log with correct action, user and ip`() {
        interactionService.recordAuthSuccess(userId, ip)

        verify(interactionLogRepository).save(argThat { action == "AUTH SUCCESS" && this.ip == ip && user == userId })
    }

    @Test
    fun `recordAuthLogout saves log with correct action, user and ip`() {
        interactionService.recordAuthLogout(userId, ip)

        verify(interactionLogRepository).save(argThat { action == "AUTH LOGOUT" && this.ip == ip && user == userId })
    }

    @Test
    fun `recordGetConfig saves log with correct action, user and ip`() {
        interactionService.recordGetConfig(userId, ip)

        verify(interactionLogRepository).save(argThat { action == "GET CONFIG" && this.ip == ip && user == userId })
    }

    @Test
    fun `recordUpdateConfig saves log with correct action, user and ip`() {
        interactionService.recordUpdateConfig(userId, ip)

        verify(interactionLogRepository).save(argThat { action == "UPDATE CONFIG" && this.ip == ip && user == userId })
    }

    @Test
    fun `recordUpdateWeather saves log with correct action, user and ip`() {
        interactionService.recordUpdateWeather(userId, ip)

        verify(interactionLogRepository).save(argThat { action == "UPDATE WEATHER" && this.ip == ip && user == userId })
    }

    @Test
    fun `repository exception does not propagate`() {
        whenever(interactionLogRepository.save(any<InteractionLog>())).thenThrow(RuntimeException("DB error"))

        interactionService.recordAuthFailed(ip)
        // no exception thrown
    }
}
