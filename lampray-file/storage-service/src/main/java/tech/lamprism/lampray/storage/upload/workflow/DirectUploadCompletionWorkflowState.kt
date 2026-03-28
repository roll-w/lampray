package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.routing.StorageWritePlan
import tech.lamprism.lampray.storage.store.BlobObject
import tech.lamprism.lampray.storage.store.BlobStore

data class DirectUploadCompletionWorkflowState(
    var writePlan: StorageWritePlan? = null,
    var groupSettings: StorageGroupConfig? = null,
    var primaryBlobStore: BlobStore? = null,
    var uploadedObject: BlobObject? = null,
    var expectedChecksum: String? = null,
    var actualChecksum: String? = null,
    var preparedBlob: PreparedBlobMaterialization? = null,
    var result: FileStorage? = null,
)
