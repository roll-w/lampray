package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class PersistTrustedUploadRunPostPersistStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    private final StorageFilePersistencePostPersistService storageFilePersistencePostPersistService;

    PersistTrustedUploadRunPostPersistStep(StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.storageFilePersistencePostPersistService = storageFilePersistencePostPersistService;
    }

    @Override
    public int getOrder() {
        return 200;
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
