package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class TrustedUploadPrepareMaterializationStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private final StorageBlobMaterializationService storageBlobMaterializationService;

    TrustedUploadPrepareMaterializationStep(StorageBlobMaterializationService storageBlobMaterializationService) {
        this.storageBlobMaterializationService = storageBlobMaterializationService;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) throws IOException {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forTempUpload(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                        tempUpload.getSize(),
                        tempUpload.getChecksumSha256(),
                        tempUpload.getPath()
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }
}
