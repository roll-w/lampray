package tech.lamprism.lampray.storage.persistence.file.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.persistence.file.PersistedMaterialization

/**
 * @author RollW
 */
data class PersistTrustedUploadWorkflowContext @JvmOverloads constructor(
    val groupName: String,
    val fileName: String,
    val mimeType: String,
    val fileType: FileType,
    val ownerUserId: Long?,
    val preparedBlob: PreparedBlobMaterialization,
    val state: PersistTrustedUploadWorkflowState = PersistTrustedUploadWorkflowState(),
)
