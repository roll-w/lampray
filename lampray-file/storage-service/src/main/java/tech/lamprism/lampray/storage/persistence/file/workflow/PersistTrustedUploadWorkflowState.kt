package tech.lamprism.lampray.storage.persistence.file.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.persistence.file.PersistedMaterialization

/**
 * @author RollW
 */
data class PersistTrustedUploadWorkflowState(
    var persistedMaterialization: PersistedMaterialization? = null,
    var result: FileStorage? = null,
)
