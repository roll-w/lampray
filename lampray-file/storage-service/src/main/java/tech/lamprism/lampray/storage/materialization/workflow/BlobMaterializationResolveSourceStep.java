package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
@Component
final class BlobMaterializationResolveSourceStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(BlobMaterializationWorkflowContext context) {
        context.getState().setSource(context.getRequest().source());
    }
}
