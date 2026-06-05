package com.shivankkapoor.helmseek_backend.job

import com.shivankkapoor.helmseek_backend.repository.SessionRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.OffsetDateTime

class SessionCleanupJobTest {

    private val sessionRepository = mock<SessionRepository>()
    private val job = SessionCleanupJob(sessionRepository)

    @Test
    fun `deleteExpiredSessions calls repository with current time`() {
        whenever(sessionRepository.deleteExpired(any())).thenReturn(0)

        val before = OffsetDateTime.now()
        job.deleteExpiredSessions()
        val after = OffsetDateTime.now()

        val captor = argumentCaptor<OffsetDateTime>()
        verify(sessionRepository).deleteExpired(captor.capture())
        val used = captor.firstValue
        assert(!used.isBefore(before) && !used.isAfter(after))
    }

    @Test
    fun `deleteExpiredSessions with deleted rows completes without error`() {
        whenever(sessionRepository.deleteExpired(any())).thenReturn(5)
        job.deleteExpiredSessions()
        verify(sessionRepository).deleteExpired(any())
    }

    @Test
    fun `deleteExpiredSessions with no expired sessions completes without error`() {
        whenever(sessionRepository.deleteExpired(any())).thenReturn(0)
        job.deleteExpiredSessions()
        verify(sessionRepository).deleteExpired(any())
    }
}
