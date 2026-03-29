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
