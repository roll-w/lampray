package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.materialization.TempUploads;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class ProxyUploadWriteTempUploadStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    @Override
    public void execute(ProxyUploadWorkflowContext context) throws IOException {
        context.getState().setTempUpload(TempUploads.write(
                context.getInputStream(),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings").getMaxSizeBytes()
        ));
    }
}
