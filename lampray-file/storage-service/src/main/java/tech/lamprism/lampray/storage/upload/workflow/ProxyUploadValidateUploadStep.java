package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class ProxyUploadValidateUploadStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(ProxyUploadWorkflowContext context) {
        VALIDATION_RULES.validateUploadedContent(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings")
        );
    }
}
