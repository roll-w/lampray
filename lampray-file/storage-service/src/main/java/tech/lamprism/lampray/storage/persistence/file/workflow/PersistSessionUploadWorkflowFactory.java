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
public class PersistSessionUploadWorkflowFactory {
    private final WorkflowStep<PersistSessionUploadWorkflowContext> persistInTransactionStep;
    private final WorkflowStep<PersistSessionUploadWorkflowContext> runPostPersistStep;
    private final WorkflowStep<PersistSessionUploadWorkflowContext> publishResultStep;

    public PersistSessionUploadWorkflowFactory(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                               StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.persistInTransactionStep = new PersistSessionUploadPersistInTransactionStep(storageFilePersistenceTransactionService);
        this.runPostPersistStep = new PersistSessionUploadRunPostPersistStep(storageFilePersistencePostPersistService);
        this.publishResultStep = new PersistSessionUploadPublishResultStep();
    }

    public PersistSessionUploadWorkflow create() {
        return new PersistSessionUploadWorkflow(List.of(
                persistInTransactionStep,
                runPostPersistStep,
                publishResultStep
        ));
    }
}
