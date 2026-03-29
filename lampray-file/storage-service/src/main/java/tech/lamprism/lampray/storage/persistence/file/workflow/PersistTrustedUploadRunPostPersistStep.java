package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class PersistTrustedUploadRunPostPersistStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    PersistTrustedUploadRunPostPersistStep(StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    @Override
    public void execute(PersistTrustedUploadWorkflowContext context) {
        storageFilePersistencePostPersistService.afterTrustedUploadPersisted(
                Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization"),
                context.getGroupName(),
                context.getOwnerUserId()
        );
    }
}
