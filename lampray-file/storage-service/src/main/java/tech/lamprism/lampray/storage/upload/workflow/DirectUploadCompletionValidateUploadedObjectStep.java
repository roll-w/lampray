package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class DirectUploadCompletionValidateUploadedObjectStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        VALIDATION_RULES.validateUploadedObject(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings")
        );
    }
}
