package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class ProxyUploadPersistUploadStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final StorageFilePersistenceService storageFilePersistenceService;

    ProxyUploadPersistUploadStep(StorageFilePersistenceService storageFilePersistenceService) {
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    @Override
    public int getOrder() {
        return 600;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistSessionUpload(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }
}
