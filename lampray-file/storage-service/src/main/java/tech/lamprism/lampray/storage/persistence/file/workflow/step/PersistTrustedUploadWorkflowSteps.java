package tech.lamprism.lampray.storage.persistence.file.workflow.step;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.persistence.file.workflow.PersistTrustedUploadWorkflowContext;

import java.util.Objects;

@Service
public class PersistTrustedUploadWorkflowSteps {
    private final StorageFilePersistenceTransactionService storageFilePersistenceTransactionService;
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    public PersistTrustedUploadWorkflowSteps(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                             StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistenceTransactionService = storageFilePersistenceTransactionService;
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    public void persistInTransaction(PersistTrustedUploadWorkflowContext context) {
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

    public void runPostPersist(PersistTrustedUploadWorkflowContext context) {
        storageFilePersistencePostPersistService.afterTrustedUploadPersisted(
                Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization"),
                context.getGroupName(),
                context.getOwnerUserId()
        );
    }

    public void publishResult(PersistTrustedUploadWorkflowContext context) {
        context.getState().setResult(Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization").getFileStorage());
    }
}
