package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PersistSessionUploadWorkflow implements Workflow<PersistSessionUploadWorkflowContext, FileStorage> {
    private final List<WorkflowStep<PersistSessionUploadWorkflowContext>> steps;

    public PersistSessionUploadWorkflow(List<WorkflowStep<PersistSessionUploadWorkflowContext>> steps) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
    }

    @Override
    public FileStorage execute(PersistSessionUploadWorkflowContext context) {
        for (WorkflowStep<PersistSessionUploadWorkflowContext> step : steps) {
            try {
                step.execute(context);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Unexpected I/O failure in file persistence workflow", exception);
            }
        }
        return Objects.requireNonNull(context.getState().getResult(), "persistSessionUploadResult");
    }
}
