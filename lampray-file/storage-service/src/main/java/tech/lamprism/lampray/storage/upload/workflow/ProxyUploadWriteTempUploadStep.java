package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.materialization.TempUploads;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class ProxyUploadWriteTempUploadStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) throws IOException {
        context.getState().setTempUpload(TempUploads.write(
                context.getInputStream(),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings").getMaxSizeBytes()
        ));
    }
}
