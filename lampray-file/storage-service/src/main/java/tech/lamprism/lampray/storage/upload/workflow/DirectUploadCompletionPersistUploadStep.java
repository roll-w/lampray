package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class DirectUploadCompletionPersistUploadStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final StorageFilePersistenceService storageFilePersistenceService;

    DirectUploadCompletionPersistUploadStep(StorageFilePersistenceService storageFilePersistenceService) {
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistSessionUpload(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }
}
