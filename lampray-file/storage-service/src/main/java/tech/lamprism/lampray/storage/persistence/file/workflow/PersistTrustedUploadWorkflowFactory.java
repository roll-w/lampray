package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class PersistTrustedUploadWorkflowFactory {
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> persistInTransactionStep;
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> runPostPersistStep;
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> publishResultStep;

    public PersistTrustedUploadWorkflowFactory(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                               StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.persistInTransactionStep = new PersistTrustedUploadPersistInTransactionStep(storageFilePersistenceTransactionService);
        this.runPostPersistStep = new PersistTrustedUploadRunPostPersistStep(storageFilePersistencePostPersistService);
        this.publishResultStep = new PersistTrustedUploadPublishResultStep();
    }

    public PersistTrustedUploadWorkflow create() {
        return new PersistTrustedUploadWorkflow(List.of(
                persistInTransactionStep,
                runPostPersistStep,
                publishResultStep
        ));
    }
}
