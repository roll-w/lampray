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
