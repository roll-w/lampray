package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class TrustedUploadPersistUploadStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private final StorageFilePersistenceService storageFilePersistenceService;

    TrustedUploadPersistUploadStep(StorageFilePersistenceService storageFilePersistenceService) {
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistTrustedUpload(
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                null,
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }
}
