package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class PersistTrustedUploadPublishResultStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    @Override
    public void execute(PersistTrustedUploadWorkflowContext context) {
        context.getState().setResult(Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization").getFileStorage());
    }
}
