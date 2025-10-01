package tech.lamprism.lampray.web.controller.system.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * @author RollW
 */
data class ListSettingRequest(
    @field:Min(value = 1)
    val page: Int = 1,

    @field:Min(value = 1)
    @field:Max(value = 200)
    val size: Int = 20,
)