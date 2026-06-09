package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.response.LocationResponseDTO
import com.shivankkapoor.helmseek_backend.model.InteractionLog
import com.shivankkapoor.helmseek_backend.repository.InteractionLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InteractionService(
    private val interactionLogRepository: InteractionLogRepository,
    private val ipService: IpService
) {
    private final val AUTH_FAILED: String = "AUTH FAILED"
    private final val AUTH_SUCCESS: String = "AUTH SUCCESS"
    private final val AUTH_LOGOUT: String = "AUTH LOGOUT"
    private final val GET_CONFIG: String = "GET CONFIG"
    private final val UPDATE_CONFIG: String = "UPDATE CONFIG"
    private final val UPDATE_WEATHER: String = "UPDATE WEATHER"


    companion object {
        private val log = LoggerFactory.getLogger(InteractionService::class.java)
    }

    private fun recordInteraction(interactionLog: InteractionLog) {
        try{
            interactionLogRepository.save(interactionLog)
        }catch (e: Exception){
            log.error("Error while saving interaction {} from {} error ",interactionLog.action,interactionLog.ip,e)
        }
    }

    @Async
    fun recordAuthFailed(ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = AUTH_FAILED, user = null, city = geo.city, country = geo.country))
    }

    @Async
    fun recordAuthSuccess(user: UUID, ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = AUTH_SUCCESS, user = user, city = geo.city, country = geo.country))
    }

    @Async
    fun recordAuthLogout(user: UUID, ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = AUTH_LOGOUT, user = user, city = geo.city, country = geo.country))
    }

    @Async
    fun recordGetConfig(user: UUID, ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = GET_CONFIG, user = user, city = geo.city, country = geo.country))
    }

    @Async
    fun recordUpdateConfig(user: UUID, ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = UPDATE_CONFIG, user = user, city = geo.city, country = geo.country))
    }

    @Async
    fun recordUpdateWeather(user: UUID, ip:String) {
        val geo = ipService.getLocation(ip)
        recordInteraction(InteractionLog(ip=ip, action = UPDATE_WEATHER, user = user, city = geo.city, country = geo.country))
    }
}