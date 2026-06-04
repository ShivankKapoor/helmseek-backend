package com.shivankkapoor.helmseek_backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequestDTO(
    @field:NotBlank
    @field:Size(max = 32)
    val username: String,

    @field:NotBlank
    @field:Size(max = 128)
    val password: String
)
