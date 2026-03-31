package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.materialization.TempUploads;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class TrustedUploadWriteTempUploadStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) throws IOException {
        StorageGroupConfig groupSettings = Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings");
        context.getState().setTempUpload(TempUploads.write(context.getInputStream(), groupSettings.getMaxSizeBytes()));
    }
}
