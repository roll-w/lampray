package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity

/**
 * @author RollW
 */
data class DirectUploadCompletionWorkflowContext @JvmOverloads constructor(
    val uploadSession: StorageUploadSessionEntity,
    val state: DirectUploadCompletionWorkflowState = DirectUploadCompletionWorkflowState(),
)
