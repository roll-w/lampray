package tech.lamprism.lampray.storage.upload.workflow

import java.io.InputStream

/**
 * @author RollW
 */
data class TrustedUploadWorkflowContext @JvmOverloads constructor(
    val inputStream: InputStream,
    val state: TrustedUploadWorkflowState = TrustedUploadWorkflowState(),
)
