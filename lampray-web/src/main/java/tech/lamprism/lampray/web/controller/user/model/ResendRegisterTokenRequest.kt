package tech.lamprism.lampray.web.controller.user.model

import jakarta.validation.constraints.NotEmpty

/**
 * @author RollW
 */
data class ResendRegisterTokenRequest(
    @field:NotEmpty
    val username: String,
    @field:NotEmpty
    val email: String,
) {
}