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
final class ProxyUploadPublishTrafficStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final StorageTrafficPublisher storageTrafficPublisher;

    ProxyUploadPublishTrafficStep(StorageTrafficPublisher storageTrafficPublisher) {
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        storageTrafficPublisher.publishProxyUpload(context.getUploadSession().getGroupName(), tempUpload.getSize());
    }
}
