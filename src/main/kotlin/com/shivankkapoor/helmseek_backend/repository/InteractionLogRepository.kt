package com.shivankkapoor.helmseek_backend.repository

import com.shivankkapoor.helmseek_backend.model.InteractionLog
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface InteractionLogRepository : CrudRepository<InteractionLog, UUID>
