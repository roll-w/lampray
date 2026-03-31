package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.support.PathCleanupSupport;
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
public class ProxyUploadWorkflow implements Workflow<ProxyUploadWorkflowContext, FileStorage> {
    private final List<WorkflowStep<ProxyUploadWorkflowContext>> steps;

    public ProxyUploadWorkflow(List<WorkflowStep<ProxyUploadWorkflowContext>> steps) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
    }

    @Override
    public FileStorage execute(ProxyUploadWorkflowContext context) throws IOException {
        try {
            for (WorkflowStep<ProxyUploadWorkflowContext> step : steps) {
                step.execute(context);
            }
            return Objects.requireNonNull(context.getState().getResult(), "proxyUploadResult");
        } finally {
            TempUpload tempUpload = context.getState().getTempUpload();
            if (tempUpload != null) {
                PathCleanupSupport.deleteIfExistsQuietly(tempUpload.getPath());
            }
        }
    }
}
