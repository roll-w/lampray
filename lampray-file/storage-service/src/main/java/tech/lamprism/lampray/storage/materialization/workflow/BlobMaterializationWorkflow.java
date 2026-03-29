package tech.lamprism.lampray.storage.materialization.workflow;

import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public class BlobMaterializationWorkflow implements Workflow<BlobMaterializationWorkflowContext, PreparedBlobMaterialization> {
    private final List<WorkflowStep<BlobMaterializationWorkflowContext>> steps;

    public BlobMaterializationWorkflow(List<WorkflowStep<BlobMaterializationWorkflowContext>> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public PreparedBlobMaterialization execute(BlobMaterializationWorkflowContext context) throws IOException {
        for (WorkflowStep<BlobMaterializationWorkflowContext> step : steps) {
            step.execute(context);
        }
        return Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob");
    }
}
