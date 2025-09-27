package tech.lamprism.lampray.web.controller.system.model

import jakarta.validation.constraints.Min

/**
 * @author RollW
 */
data class ListSettingRequest(
    @field:Min(value = 1, message = "Page must be greater than or equal to 1")
    val page: Int = 1,

    @field:Min(value = 1, message = "Size must be between 1 and 200")
    @field:Min(value = 200, message = "Size must be between 1 and 200")
    val size: Int = 20,
) {
    init {
        require(page >= 1) { "Page must be greater than or equal to 1" }
        require(size in 1..200) { "Size must be between 1 and 200" }
    }
}