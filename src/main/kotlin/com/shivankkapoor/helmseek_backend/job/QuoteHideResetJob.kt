package com.shivankkapoor.helmseek_backend.job

import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteHideResetJob(private val userRepository: UserRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(QuoteHideResetJob::class.java)
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "America/Chicago")
    @Transactional
    fun resetHiddenQuotes() {
        log.info("[Cron triggered]: Resetting hidden quotes")
        val reset = userRepository.resetHideQuote()
        if (reset > 0) {
            log.info("Reset {} hidden quote flag(s)", reset)
        } else {
            log.info("No hidden quotes to reset")
        }
    }
}
