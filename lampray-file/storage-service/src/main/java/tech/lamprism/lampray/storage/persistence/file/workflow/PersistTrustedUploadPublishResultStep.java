package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class PersistTrustedUploadPublishResultStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void execute(PersistTrustedUploadWorkflowContext context) {
        context.getState().setResult(Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization").getFileStorage());
    }
}
