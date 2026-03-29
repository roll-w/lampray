package tech.lamprism.lampray.storage.session

import tech.lamprism.lampray.storage.StorageAccessRequest

/**
 * @author RollW
 */
data class DirectUploadProvision(
    val objectKey: String,
    val accessRequest: StorageAccessRequest,
)
