package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class TrustedUploadWorkflowFactory {
    private final WorkflowStep<TrustedUploadWorkflowContext> resolvePlanStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> writeTempUploadStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> assignDefaultFileNameStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> publishTrafficStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> validateUploadStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> prepareMaterializationStep;
    private final WorkflowStep<TrustedUploadWorkflowContext> persistUploadStep;

    public TrustedUploadWorkflowFactory(StorageWritePlanResolver storageWritePlanResolver,
                                        StorageRuntimeConfig runtimeSettings,
                                        StorageTrafficPublisher storageTrafficPublisher,
                                        StorageBlobMaterializationService storageBlobMaterializationService,
                                        StorageFilePersistenceService storageFilePersistenceService) {
        this.resolvePlanStep = new TrustedUploadResolvePlanStep(storageWritePlanResolver, runtimeSettings);
        this.writeTempUploadStep = new TrustedUploadWriteTempUploadStep();
        this.assignDefaultFileNameStep = new TrustedUploadAssignDefaultFileNameStep();
        this.publishTrafficStep = new TrustedUploadPublishTrafficStep(storageTrafficPublisher);
        this.validateUploadStep = new TrustedUploadValidateUploadStep();
        this.prepareMaterializationStep = new TrustedUploadPrepareMaterializationStep(storageBlobMaterializationService);
        this.persistUploadStep = new TrustedUploadPersistUploadStep(storageFilePersistenceService);
    }

    public TrustedUploadWorkflow create() {
        return new TrustedUploadWorkflow(List.of(
                resolvePlanStep,
                writeTempUploadStep,
                assignDefaultFileNameStep,
                publishTrafficStep,
                validateUploadStep,
                prepareMaterializationStep,
                persistUploadStep
        ));
    }
}
