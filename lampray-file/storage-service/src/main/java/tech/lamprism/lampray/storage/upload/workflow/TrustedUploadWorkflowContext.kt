package tech.lamprism.lampray.storage.upload.workflow

import java.io.InputStream

data class TrustedUploadWorkflowContext @JvmOverloads constructor(
    val inputStream: InputStream,
    val state: TrustedUploadWorkflowState = TrustedUploadWorkflowState(),
)
