package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class TrustedUploadValidateUploadStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        VALIDATION_RULES.validateUploadRequest(
                new StorageUploadRequest(
                        Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                        Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                        tempUpload.getSize(),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        tempUpload.getChecksumSha256()
                ),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType")
        );
    }
}
