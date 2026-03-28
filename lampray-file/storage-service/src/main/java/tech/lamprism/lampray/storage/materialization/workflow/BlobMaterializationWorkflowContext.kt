package tech.lamprism.lampray.storage.materialization.workflow

import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest

data class BlobMaterializationWorkflowContext @JvmOverloads constructor(
    val request: BlobMaterializationRequest,
    val state: BlobMaterializationWorkflowState = BlobMaterializationWorkflowState(),
)
