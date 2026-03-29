package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
final class ProxyUploadResolvePlanStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final StorageWritePlanResolver storageWritePlanResolver;

    ProxyUploadResolvePlanStep(StorageWritePlanResolver storageWritePlanResolver) {
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) {
        StorageWritePlan writePlan = storageWritePlanResolver.restore(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }
}
