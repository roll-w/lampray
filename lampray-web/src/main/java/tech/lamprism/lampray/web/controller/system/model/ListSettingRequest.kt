package tech.lamprism.lampray.web.controller.system.model

/**
 * @author RollW
 */
data class ListSettingRequest(
    val page: Int = 1,
    val size: Int = 20,
) {
}