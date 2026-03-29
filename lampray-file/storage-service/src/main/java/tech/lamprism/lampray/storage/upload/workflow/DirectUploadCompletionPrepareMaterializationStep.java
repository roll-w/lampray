package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class DirectUploadCompletionPrepareMaterializationStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final StorageBlobMaterializationService storageBlobMaterializationService;

    DirectUploadCompletionPrepareMaterializationStep(StorageBlobMaterializationService storageBlobMaterializationService) {
        this.storageBlobMaterializationService = storageBlobMaterializationService;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forUploadedObject(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        context.getUploadSession().getMimeType(),
                        context.getUploadSession().getFileType(),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject").getSize(),
                        Objects.requireNonNull(context.getState().getActualChecksum(), "actualChecksum"),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject")
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }
}
