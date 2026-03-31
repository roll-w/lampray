package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionValidateUploadedObjectStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        VALIDATION_RULES.validateUploadedObject(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings")
        );
    }
}
