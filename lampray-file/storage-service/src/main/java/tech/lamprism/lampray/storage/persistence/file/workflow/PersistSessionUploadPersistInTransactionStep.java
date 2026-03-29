package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
final class PersistSessionUploadPersistInTransactionStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    private final StorageFilePersistenceTransactionService storageFilePersistenceTransactionService;

    PersistSessionUploadPersistInTransactionStep(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService) {
        this.storageFilePersistenceTransactionService = storageFilePersistenceTransactionService;
    }

    @Override
    public void execute(PersistSessionUploadWorkflowContext context) {
        context.getState().setPersistedMaterialization(
                storageFilePersistenceTransactionService.persistSessionUpload(context.getUploadSession(), context.getPreparedBlob())
        );
    }
}
