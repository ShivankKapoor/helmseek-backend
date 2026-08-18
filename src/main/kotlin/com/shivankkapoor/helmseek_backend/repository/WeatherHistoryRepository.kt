package com.shivankkapoor.helmseek_backend.repository

import com.shivankkapoor.helmseek_backend.model.WeatherHistory
import org.springframework.data.repository.CrudRepository

interface WeatherHistoryRepository : CrudRepository<WeatherHistory, Long>