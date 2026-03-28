package tech.lamprism.lampray.storage.session.workflow;

import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CreateUploadSessionWorkflow implements Workflow<CreateUploadSessionWorkflowContext, StorageUploadSession> {
    private final List<WorkflowStep<CreateUploadSessionWorkflowContext>> steps;

    public CreateUploadSessionWorkflow(List<WorkflowStep<CreateUploadSessionWorkflowContext>> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public StorageUploadSession execute(CreateUploadSessionWorkflowContext context) throws IOException {
        for (WorkflowStep<CreateUploadSessionWorkflowContext> step : steps) {
            step.execute(context);
        }
        return Objects.requireNonNull(context.getState().getResult(), "createUploadSessionResult");
    }
}
