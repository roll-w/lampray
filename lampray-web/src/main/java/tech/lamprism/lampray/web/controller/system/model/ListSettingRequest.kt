package tech.lamprism.lampray.web.controller.system.model

/**
 * @author RollW
 */
data class ListSettingRequest(
    val page: Int = 1,
    val size: Int = 20,
) {
    init {
        require(page >= 1) { "Page must be greater than or equal to 1" }
        require(size in 1..200) { "Size must be between 1 and 200" }
    }
}