package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
final class PersistTrustedUploadPersistInTransactionStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    private final StorageFilePersistenceTransactionService storageFilePersistenceTransactionService;

    PersistTrustedUploadPersistInTransactionStep(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService) {
        this.storageFilePersistenceTransactionService = storageFilePersistenceTransactionService;
    }

    @Override
    public void execute(PersistTrustedUploadWorkflowContext context) {
        context.getState().setPersistedMaterialization(
                storageFilePersistenceTransactionService.persistTrustedUpload(
                        context.getGroupName(),
                        context.getFileName(),
                        context.getMimeType(),
                        context.getFileType(),
                        context.getOwnerUserId(),
                        context.getPreparedBlob()
                )
        );
    }
}
