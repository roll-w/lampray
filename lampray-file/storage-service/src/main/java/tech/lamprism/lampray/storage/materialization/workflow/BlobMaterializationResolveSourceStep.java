package tech.lamprism.lampray.storage.materialization.workflow;

import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
final class BlobMaterializationResolveSourceStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    @Override
    public void execute(BlobMaterializationWorkflowContext context) {
        context.getState().setSource(context.getRequest().source());
    }
}
