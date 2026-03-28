package tech.lamprism.lampray.storage.session

import tech.lamprism.lampray.storage.StorageAccessRequest

data class DirectUploadProvision(
    val objectKey: String,
    val accessRequest: StorageAccessRequest,
)
