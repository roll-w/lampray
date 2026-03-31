package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
@Component
final class ProxyUploadResolvePlanStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final StorageWritePlanResolver storageWritePlanResolver;

    ProxyUploadResolvePlanStep(StorageWritePlanResolver storageWritePlanResolver) {
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public int getOrder() {
        return 100;
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
