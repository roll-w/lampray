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
