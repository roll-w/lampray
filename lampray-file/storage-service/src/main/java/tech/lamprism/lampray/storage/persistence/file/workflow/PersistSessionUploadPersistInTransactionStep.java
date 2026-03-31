package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
@Component
final class PersistSessionUploadPersistInTransactionStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    private final StorageFilePersistenceTransactionService storageFilePersistenceTransactionService;

    PersistSessionUploadPersistInTransactionStep(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService) {
        this.storageFilePersistenceTransactionService = storageFilePersistenceTransactionService;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(PersistSessionUploadWorkflowContext context) {
        context.getState().setPersistedMaterialization(
                storageFilePersistenceTransactionService.persistSessionUpload(context.getUploadSession(), context.getPreparedBlob())
        );
    }
}
