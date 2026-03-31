package tech.lamprism.lampray.storage.session.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class CreateUploadSessionBuildResultStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    @Override
    public int getOrder() {
        return 600;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        context.getState().setResult(new StorageUploadSession(
                Objects.requireNonNull(context.getState().getUploadId(), "uploadId"),
                Objects.requireNonNull(context.getState().getUploadMode(), "uploadMode"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileId(), "fileId"),
                context.getState().getDirectRequest(),
                Objects.requireNonNull(context.getState().getExpiresAt(), "expiresAt")
        ));
    }
}
