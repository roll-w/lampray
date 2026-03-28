package tech.lamprism.lampray.storage.materialization.workflow

import tech.lamprism.lampray.storage.materialization.BlobMaterializationSource
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity

data class BlobMaterializationWorkflowState(
    var source: BlobMaterializationSource? = null,
    var existingBlob: StorageBlobEntity? = null,
    var primaryObjectKey: String? = null,
    var preparedBlob: PreparedBlobMaterialization? = null,
    val materializedPlacements: MutableMap<String, String> = linkedMapOf(),
)
