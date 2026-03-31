package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class PersistSessionUploadRunPostPersistStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    PersistSessionUploadRunPostPersistStep(StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    @Override
    public int getOrder() {
        return 200;
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
