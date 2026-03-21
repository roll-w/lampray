package tech.lamprism.lampray.web.controller.storage.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class StorageAdminFileListRequest(
    @field:Min(1)
    val page: Int = 1,

    @field:Min(1)
    @field:Max(200)
    val size: Int = 20,

    val groupName: String? = null,

    val ownerUserId: Long? = null,

    val fileName: String? = null,
)
