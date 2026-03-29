package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class PersistSessionUploadPublishResultStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    @Override
    public void execute(PersistSessionUploadWorkflowContext context) {
        context.getState().setResult(Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization").getFileStorage());
    }
}
