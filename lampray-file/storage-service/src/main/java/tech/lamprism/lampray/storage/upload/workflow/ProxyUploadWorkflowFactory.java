package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
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
public class ProxyUploadWorkflowFactory {
    private final WorkflowStep<ProxyUploadWorkflowContext> resolvePlanStep;
    private final WorkflowStep<ProxyUploadWorkflowContext> writeTempUploadStep;
    private final WorkflowStep<ProxyUploadWorkflowContext> publishTrafficStep;
    private final WorkflowStep<ProxyUploadWorkflowContext> validateUploadStep;
    private final WorkflowStep<ProxyUploadWorkflowContext> prepareMaterializationStep;
    private final WorkflowStep<ProxyUploadWorkflowContext> persistUploadStep;

    public ProxyUploadWorkflowFactory(StorageBlobMaterializationService storageBlobMaterializationService,
                                      StorageFilePersistenceService storageFilePersistenceService,
                                      StorageWritePlanResolver storageWritePlanResolver,
                                      StorageTrafficPublisher storageTrafficPublisher) {
        this.resolvePlanStep = new ProxyUploadResolvePlanStep(storageWritePlanResolver);
        this.writeTempUploadStep = new ProxyUploadWriteTempUploadStep();
        this.publishTrafficStep = new ProxyUploadPublishTrafficStep(storageTrafficPublisher);
        this.validateUploadStep = new ProxyUploadValidateUploadStep();
        this.prepareMaterializationStep = new ProxyUploadPrepareMaterializationStep(storageBlobMaterializationService);
        this.persistUploadStep = new ProxyUploadPersistUploadStep(storageFilePersistenceService);
    }

    public ProxyUploadWorkflow create() {
        return new ProxyUploadWorkflow(List.of(
                resolvePlanStep,
                writeTempUploadStep,
                publishTrafficStep,
                validateUploadStep,
                prepareMaterializationStep,
                persistUploadStep
        ));
    }
}
