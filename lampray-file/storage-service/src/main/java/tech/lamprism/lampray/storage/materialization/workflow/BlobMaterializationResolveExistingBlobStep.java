package tech.lamprism.lampray.storage.materialization.workflow;

import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Optional;

/**
 * @author RollW
 */
final class BlobMaterializationResolveExistingBlobStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageBlobRepository storageBlobRepository;

    BlobMaterializationResolveExistingBlobStep(StorageRuntimeConfig runtimeSettings,
                                              StorageBlobRepository storageBlobRepository) {
        this.runtimeSettings = runtimeSettings;
        this.storageBlobRepository = storageBlobRepository;
    }

    @Override
    public void execute(BlobMaterializationWorkflowContext context) {
        Optional<StorageBlobEntity> existingBlob = runtimeSettings.getDeduplicationEnabled()
                ? storageBlobRepository.findByChecksumSha256(context.getRequest().checksum())
                : Optional.empty();
        existingBlob.ifPresent(context.getState()::setExistingBlob);
    }
}
