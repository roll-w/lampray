/*
 * Copyright (C) 2023-2026 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationSource;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementCleanupService;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementWriter;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
@Component
final class BlobMaterializationPreparePlacementsStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final BlobPlacementWriter blobPlacementWriter;
    private final BlobPlacementCleanupService blobPlacementCleanupService;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;

    BlobMaterializationPreparePlacementsStep(BlobObjectKeyFactory blobObjectKeyFactory,
                                             BlobPlacementWriter blobPlacementWriter,
                                             BlobPlacementCleanupService blobPlacementCleanupService,
                                             StorageBlobPlacementRepository storageBlobPlacementRepository) {
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.blobPlacementWriter = blobPlacementWriter;
        this.blobPlacementCleanupService = blobPlacementCleanupService;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void execute(BlobMaterializationWorkflowContext context) throws IOException {
        if (context.getState().getExistingBlob() != null) {
            context.getState().getMaterializedPlacements().putAll(ensureRequiredPlacements(context));
            return;
        }
        materializeNewBlob(context);
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
