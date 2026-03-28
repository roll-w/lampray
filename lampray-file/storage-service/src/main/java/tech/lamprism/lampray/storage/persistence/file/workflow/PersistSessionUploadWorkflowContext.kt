package tech.lamprism.lampray.storage.persistence.file.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity
import tech.lamprism.lampray.storage.persistence.file.PersistedMaterialization

data class PersistSessionUploadWorkflowContext @JvmOverloads constructor(
    val uploadSession: StorageUploadSessionEntity,
    val preparedBlob: PreparedBlobMaterialization,
    val state: PersistSessionUploadWorkflowState = PersistSessionUploadWorkflowState(),
)
