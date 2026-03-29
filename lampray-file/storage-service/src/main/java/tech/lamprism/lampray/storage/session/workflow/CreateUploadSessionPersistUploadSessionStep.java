package tech.lamprism.lampray.storage.session.workflow;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.session.StorageUploadSessionEntityFactory;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionPersistUploadSessionStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageUploadSessionEntityFactory storageUploadSessionEntityFactory;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    CreateUploadSessionPersistUploadSessionStep(StorageUploadSessionEntityFactory storageUploadSessionEntityFactory,
                                                StorageUploadSessionRepository storageUploadSessionRepository,
                                                PlatformTransactionManager transactionManager) {
        this.storageUploadSessionEntityFactory = storageUploadSessionEntityFactory;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        StorageUploadSessionEntity uploadSessionEntity = storageUploadSessionEntityFactory.createPendingSession(
                Objects.requireNonNull(context.getState().getUploadId(), "uploadId"),
                Objects.requireNonNull(context.getState().getFileId(), "fileId"),
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                context.getRequest().getSize(),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                context.getState().getChecksum(),
                context.getUserId(),
                Objects.requireNonNull(context.getState().getPrimaryBackend(), "primaryBackend"),
                context.getState().getObjectKey(),
                Objects.requireNonNull(context.getState().getUploadMode(), "uploadMode"),
                Objects.requireNonNull(context.getState().getExpiresAt(), "expiresAt"),
                Objects.requireNonNull(context.getState().getNow(), "now")
        );
        transactionTemplate.executeWithoutResult(status -> context.getState().setUploadSessionEntity(save(uploadSessionEntity)));
    }

    private StorageUploadSessionEntity save(StorageUploadSessionEntity uploadSessionEntity) {
        return storageUploadSessionRepository.save(uploadSessionEntity);
    }
}
