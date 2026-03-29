package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class DirectUploadCompletionWorkflowFactory {
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> resolvePlanStep;
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> resolveUploadedObjectStep;
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> validateUploadedObjectStep;
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> recoverAndVerifyChecksumStep;
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> prepareMaterializationStep;
    private final WorkflowStep<DirectUploadCompletionWorkflowContext> persistUploadStep;

    public DirectUploadCompletionWorkflowFactory(BlobStoreLocator blobStoreLocator,
                                                 StorageWritePlanResolver storageWritePlanResolver,
                                                 StorageBlobMaterializationService storageBlobMaterializationService,
                                                 StorageFilePersistenceService storageFilePersistenceService) {
        this.resolvePlanStep = new DirectUploadCompletionResolvePlanStep(storageWritePlanResolver);
        this.resolveUploadedObjectStep = new DirectUploadCompletionResolveUploadedObjectStep(blobStoreLocator);
        this.validateUploadedObjectStep = new DirectUploadCompletionValidateUploadedObjectStep();
        this.recoverAndVerifyChecksumStep = new DirectUploadCompletionRecoverAndVerifyChecksumStep();
        this.prepareMaterializationStep = new DirectUploadCompletionPrepareMaterializationStep(storageBlobMaterializationService);
        this.persistUploadStep = new DirectUploadCompletionPersistUploadStep(storageFilePersistenceService);
    }

    public DirectUploadCompletionWorkflow create() {
        return new DirectUploadCompletionWorkflow(List.of(
                resolvePlanStep,
                resolveUploadedObjectStep,
                validateUploadedObjectStep,
                recoverAndVerifyChecksumStep,
                prepareMaterializationStep,
                persistUploadStep
        ));
    }
}
