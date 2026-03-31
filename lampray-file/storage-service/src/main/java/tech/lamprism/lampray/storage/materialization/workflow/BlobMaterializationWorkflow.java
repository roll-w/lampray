package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class BlobMaterializationWorkflow implements Workflow<BlobMaterializationWorkflowContext, PreparedBlobMaterialization> {
    private final List<WorkflowStep<BlobMaterializationWorkflowContext>> steps;

    public BlobMaterializationWorkflow(List<WorkflowStep<BlobMaterializationWorkflowContext>> steps) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
    }

    @Override
    public PreparedBlobMaterialization execute(BlobMaterializationWorkflowContext context) throws IOException {
        for (WorkflowStep<BlobMaterializationWorkflowContext> step : steps) {
            step.execute(context);
        }
        return Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob");
    }
}
