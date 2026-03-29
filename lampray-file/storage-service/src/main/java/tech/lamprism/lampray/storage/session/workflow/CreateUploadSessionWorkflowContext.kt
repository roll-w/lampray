package tech.lamprism.lampray.storage.session.workflow

import tech.lamprism.lampray.storage.StorageUploadRequest

/**
 * @author RollW
 */
data class CreateUploadSessionWorkflowContext @JvmOverloads constructor(
    val request: StorageUploadRequest,
    val userId: Long?,
    val state: CreateUploadSessionWorkflowState = CreateUploadSessionWorkflowState(),
)
