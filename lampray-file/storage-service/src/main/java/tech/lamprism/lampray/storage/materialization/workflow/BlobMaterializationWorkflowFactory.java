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
