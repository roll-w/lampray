package tech.lamprism.lampray.storage.materialization.workflow;

import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class BlobMaterializationBuildPreparedBlobStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    @Override
    public void execute(BlobMaterializationWorkflowContext context) {
        BlobMaterializationRequest request = context.getRequest();
        if (context.getState().getExistingBlob() != null) {
            context.getState().setPreparedBlob(PreparedBlobMaterialization.existing(
                    context.getState().getExistingBlob(),
                    request.size(),
                    request.mimeType(),
                    request.fileType(),
                    context.getState().getMaterializedPlacements()
            ));
            return;
        }
        context.getState().setPreparedBlob(PreparedBlobMaterialization.newBlob(
                request.checksum(),
                request.size(),
                request.mimeType(),
                request.fileType(),
                request.primaryBackend(),
                Objects.requireNonNull(context.getState().getPrimaryObjectKey(), "primaryObjectKey"),
                context.getState().getMaterializedPlacements()
        ));
    }
}
