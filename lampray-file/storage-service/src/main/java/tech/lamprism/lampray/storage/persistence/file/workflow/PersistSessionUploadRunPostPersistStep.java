package tech.lamprism.lampray.storage.persistence.file.workflow;

import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class PersistSessionUploadRunPostPersistStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    PersistSessionUploadRunPostPersistStep(StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    @Override
    public void execute(PersistSessionUploadWorkflowContext context) {
        storageFilePersistencePostPersistService.afterSessionUploadPersisted(
                Objects.requireNonNull(context.getState().getPersistedMaterialization(), "persistedMaterialization"),
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getOwnerUserId()
        );
    }
}
