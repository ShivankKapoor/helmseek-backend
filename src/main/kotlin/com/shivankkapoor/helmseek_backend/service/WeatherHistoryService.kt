package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.WeatherHistory
import com.shivankkapoor.helmseek_backend.repository.WeatherHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class WeatherHistoryService (private val weatherHistoryRepository: WeatherHistoryRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(WeatherHistoryService::class.java)
    }

    @Async
    fun recordWeatherHistory(weather: WeatherHistory){
        try{
            weatherHistoryRepository.save(weather)
        }catch(e: Exception){
            log.error("Error while saving weather in history table",e)
        }
    }
}