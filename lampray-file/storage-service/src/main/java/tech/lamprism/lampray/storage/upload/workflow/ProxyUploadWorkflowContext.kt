package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity
import java.io.InputStream

data class ProxyUploadWorkflowContext @JvmOverloads constructor(
    val uploadSession: StorageUploadSessionEntity,
    val inputStream: InputStream,
    val state: ProxyUploadWorkflowState = ProxyUploadWorkflowState(),
)
