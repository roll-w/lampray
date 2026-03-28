package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class DirectUploadCompletionWorkflow implements Workflow<DirectUploadCompletionWorkflowContext, FileStorage> {
    private final List<WorkflowStep<DirectUploadCompletionWorkflowContext>> steps;

    public DirectUploadCompletionWorkflow(List<WorkflowStep<DirectUploadCompletionWorkflowContext>> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public FileStorage execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        for (WorkflowStep<DirectUploadCompletionWorkflowContext> step : steps) {
            step.execute(context);
        }
        return Objects.requireNonNull(context.getState().getResult(), "directUploadCompletionResult");
    }
}
