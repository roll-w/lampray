package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.materialization.TempUpload
import tech.lamprism.lampray.storage.routing.StorageWritePlan

/**
 * @author RollW
 */
data class ProxyUploadWorkflowState(
    var writePlan: StorageWritePlan? = null,
    var groupSettings: StorageGroupConfig? = null,
    var tempUpload: TempUpload? = null,
    var preparedBlob: PreparedBlobMaterialization? = null,
    var result: FileStorage? = null,
)
