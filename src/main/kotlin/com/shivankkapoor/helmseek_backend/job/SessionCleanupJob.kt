package com.shivankkapoor.helmseek_backend.job

import com.shivankkapoor.helmseek_backend.repository.SessionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Component
class SessionCleanupJob(private val sessionRepository: SessionRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(SessionCleanupJob::class.java)
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun deleteExpiredSessions() {
        val deleted = sessionRepository.deleteExpired(OffsetDateTime.now())
        if (deleted > 0) log.info("Deleted {} expired session(s)", deleted)
    }
}
