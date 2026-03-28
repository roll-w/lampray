package tech.lamprism.lampray.storage.persistence.file.workflow.step;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.persistence.file.workflow.PersistSessionUploadWorkflowContext;

import java.util.Objects;

@Service
public class PersistSessionUploadWorkflowSteps {
    private final StorageFilePersistenceTransactionService storageFilePersistenceTransactionService;
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    public PersistSessionUploadWorkflowSteps(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                             StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistenceTransactionService = storageFilePersistenceTransactionService;
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    public void persistInTransaction(PersistSessionUploadWorkflowContext context) {
        context.getState().setPersistedMaterialization(
                storageFilePersistenceTransactionService.persistSessionUpload(context.getUploadSession(), context.getPreparedBlob())
        );
    }

    public void runPostPersist(PersistSessionUploadWorkflowContext context) {
        storageFilePersistencePostPersistService.afterSessionUploadPersisted(
                Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization"),
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getOwnerUserId()
        );
    }

    public void publishResult(PersistSessionUploadWorkflowContext context) {
        context.getState().setResult(Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization").getFileStorage());
    }
}
