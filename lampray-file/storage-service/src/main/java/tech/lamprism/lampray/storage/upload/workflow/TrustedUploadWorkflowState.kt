package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.materialization.TempUpload
import tech.lamprism.lampray.storage.routing.StorageWritePlan

/**
 * @author RollW
 */
data class TrustedUploadWorkflowState(
    var groupName: String? = null,
    var writePlan: StorageWritePlan? = null,
    var groupSettings: StorageGroupConfig? = null,
    var mimeType: String? = null,
    var fileType: FileType? = null,
    var tempUpload: TempUpload? = null,
    var fileName: String? = null,
    var preparedBlob: PreparedBlobMaterialization? = null,
    var result: FileStorage? = null,
)
