package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementCleanupService;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementWriter;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class BlobMaterializationWorkflowFactory {
    private final WorkflowStep<BlobMaterializationWorkflowContext> resolveSourceStep;
    private final WorkflowStep<BlobMaterializationWorkflowContext> resolveExistingBlobStep;
    private final WorkflowStep<BlobMaterializationWorkflowContext> preparePlacementsStep;
    private final WorkflowStep<BlobMaterializationWorkflowContext> buildPreparedBlobStep;

    public BlobMaterializationWorkflowFactory(StorageRuntimeConfig runtimeSettings,
                                              StorageBlobRepository storageBlobRepository,
                                              StorageBlobPlacementRepository storageBlobPlacementRepository,
                                              BlobObjectKeyFactory blobObjectKeyFactory,
                                              BlobPlacementWriter blobPlacementWriter,
                                              BlobPlacementCleanupService blobPlacementCleanupService) {
        this.resolveSourceStep = new BlobMaterializationResolveSourceStep();
        this.resolveExistingBlobStep = new BlobMaterializationResolveExistingBlobStep(runtimeSettings, storageBlobRepository);
        this.preparePlacementsStep = new BlobMaterializationPreparePlacementsStep(
                blobObjectKeyFactory,
                blobPlacementWriter,
                blobPlacementCleanupService,
                storageBlobPlacementRepository
        );
        this.buildPreparedBlobStep = new BlobMaterializationBuildPreparedBlobStep();
    }

    public BlobMaterializationWorkflow create() {
        return new BlobMaterializationWorkflow(List.of(
                resolveSourceStep,
                resolveExistingBlobStep,
                preparePlacementsStep,
                buildPreparedBlobStep
        ));
    }
}
