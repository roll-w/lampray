package tech.lamprism.lampray.storage.materialization.workflow.step;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationSource;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementCleanupService;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementWriter;
import tech.lamprism.lampray.storage.materialization.workflow.BlobMaterializationWorkflowContext;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class BlobMaterializationWorkflowSteps {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final BlobPlacementWriter blobPlacementWriter;
    private final BlobPlacementCleanupService blobPlacementCleanupService;

    public BlobMaterializationWorkflowSteps(StorageRuntimeConfig runtimeSettings,
                                           StorageBlobRepository storageBlobRepository,
                                           StorageBlobPlacementRepository storageBlobPlacementRepository,
                                           BlobObjectKeyFactory blobObjectKeyFactory,
                                           BlobPlacementWriter blobPlacementWriter,
                                           BlobPlacementCleanupService blobPlacementCleanupService) {
        this.runtimeSettings = runtimeSettings;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.blobPlacementWriter = blobPlacementWriter;
        this.blobPlacementCleanupService = blobPlacementCleanupService;
    }

    public void resolveSource(BlobMaterializationWorkflowContext context) {
        context.getState().setSource(context.getRequest().source());
    }

    public void resolveExistingBlob(BlobMaterializationWorkflowContext context) {
        Optional<StorageBlobEntity> existingBlob = runtimeSettings.getDeduplicationEnabled()
                ? storageBlobRepository.findByChecksumSha256(context.getRequest().checksum())
                : Optional.empty();
        existingBlob.ifPresent(context.getState()::setExistingBlob);
    }

    public void preparePlacements(BlobMaterializationWorkflowContext context) throws IOException {
        if (context.getState().getExistingBlob() != null) {
            context.getState().getMaterializedPlacements().putAll(ensureRequiredPlacements(context));
            return;
        }
        materializeNewBlob(context);
    }

    public void buildPreparedBlob(BlobMaterializationWorkflowContext context) {
        BlobMaterializationRequest request = context.getRequest();
        if (context.getState().getExistingBlob() != null) {
            context.getState().setPreparedBlob(PreparedBlobMaterialization.existing(
                    context.getState().getExistingBlob(),
                    request.size(),
                    request.mimeType(),
                    request.fileType(),
                    context.getState().getMaterializedPlacements()
            ));
            return;
        }
        context.getState().setPreparedBlob(PreparedBlobMaterialization.newBlob(
                request.checksum(),
                request.size(),
                request.mimeType(),
                request.fileType(),
                request.primaryBackend(),
                Objects.requireNonNull(context.getState().getPrimaryObjectKey(), "primaryObjectKey"),
                context.getState().getMaterializedPlacements()
        ));
    }

    private void materializeNewBlob(BlobMaterializationWorkflowContext context) throws IOException {
        BlobMaterializationRequest request = context.getRequest();
        BlobMaterializationSource source = Objects.requireNonNull(context.getState().getSource(), "source");
        String primaryObjectKey = source.resolvePrimaryObjectKey(blobObjectKeyFactory, request.checksum());
        context.getState().setPrimaryObjectKey(primaryObjectKey);
        try {
            source.materializePrimary(blobPlacementWriter, request, primaryObjectKey);
            context.getState().getMaterializedPlacements().put(request.primaryBackend(), primaryObjectKey);

            if (request.writePlan().getGroupSettings().getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
                for (String mirrorBackend : request.writePlan().getMirrorBackends()) {
                    if (context.getState().getMaterializedPlacements().containsKey(mirrorBackend)) {
                        continue;
                    }
                    materializePlacement(
                            context.getState().getMaterializedPlacements(),
                            request,
                            mirrorBackend,
                            blobObjectKeyFactory.createKey(request.checksum()),
                            request.primaryBackend(),
                            primaryObjectKey,
                            source
                    );
                }
            }
        } catch (IOException | RuntimeException exception) {
            blobPlacementCleanupService.cleanup(
                    context.getState().getMaterializedPlacements(),
                    source.protectsPrimaryPlacement() ? request.primaryBackend() : null,
                    source.protectsPrimaryPlacement() ? primaryObjectKey : null
            );
            throw exception;
        }
    }

    private Map<String, String> ensureRequiredPlacements(BlobMaterializationWorkflowContext context) throws IOException {
        BlobMaterializationRequest request = context.getRequest();
        StorageBlobEntity existingBlob = Objects.requireNonNull(context.getState().getExistingBlob(), "existingBlob");
        Map<String, String> placementsToPersist = new LinkedHashMap<>();
        Set<String> requiredBackends = new LinkedHashSet<>();
        requiredBackends.add(request.primaryBackend());
        if (request.writePlan().getGroupSettings().getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
            requiredBackends.addAll(request.writePlan().getMirrorBackends());
        }

        BlobMaterializationSource source = Objects.requireNonNull(context.getState().getSource(), "source");
        String sourceBackend = source.resolveSourceBackend(existingBlob, request);
        String sourceObjectKey = source.resolveSourceObjectKey(existingBlob, request, existingBlob.getPrimaryObjectKey());

        try {
            for (String backendName : requiredBackends) {
                if (storageBlobPlacementRepository.findByBlobIdAndBackendName(existingBlob.getBlobId(), backendName).isPresent()) {
                    continue;
                }
                String objectKey = backendName.equals(sourceBackend)
                        ? sourceObjectKey
                        : blobObjectKeyFactory.createKey(request.checksum());
                materializePlacement(
                        placementsToPersist,
                        request,
                        backendName,
                        objectKey,
                        sourceBackend,
                        sourceObjectKey,
                        source
                );
            }
        } catch (IOException | RuntimeException exception) {
            blobPlacementCleanupService.cleanup(placementsToPersist, sourceBackend, sourceObjectKey);
            throw exception;
        }
        return placementsToPersist;
    }

    private void materializePlacement(Map<String, String> materializedPlacements,
                                      BlobMaterializationRequest request,
                                      String backendName,
                                      String objectKey,
                                      String sourceBackend,
                                      String sourceObjectKey,
                                      BlobMaterializationSource source) throws IOException {
        if (backendName.equals(sourceBackend)) {
            materializedPlacements.put(backendName, objectKey);
            return;
        }
        source.materializeReplica(blobPlacementWriter, request, backendName, objectKey, sourceBackend, sourceObjectKey);
        materializedPlacements.put(backendName, objectKey);
    }
}
