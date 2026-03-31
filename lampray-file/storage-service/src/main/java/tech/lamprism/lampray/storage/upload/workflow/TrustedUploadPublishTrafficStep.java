package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class TrustedUploadPublishTrafficStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private final StorageTrafficPublisher storageTrafficPublisher;

    TrustedUploadPublishTrafficStep(StorageTrafficPublisher storageTrafficPublisher) {
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        storageTrafficPublisher.publishProxyUpload(Objects.requireNonNull(context.getState().getGroupName(), "groupName"), tempUpload.getSize());
    }
}
