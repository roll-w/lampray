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

package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistencePostPersistService;
import tech.lamprism.lampray.storage.persistence.file.StorageFilePersistenceTransactionService;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class PersistTrustedUploadWorkflowFactory {
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> persistInTransactionStep;
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> runPostPersistStep;
    private final WorkflowStep<PersistTrustedUploadWorkflowContext> publishResultStep;

    public PersistTrustedUploadWorkflowFactory(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                               StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.persistInTransactionStep = new PersistTrustedUploadPersistInTransactionStep(storageFilePersistenceTransactionService);
        this.runPostPersistStep = new PersistTrustedUploadRunPostPersistStep(storageFilePersistencePostPersistService);
        this.publishResultStep = new PersistTrustedUploadPublishResultStep();
    }

    public PersistTrustedUploadWorkflow create() {
        return new PersistTrustedUploadWorkflow(List.of(
                persistInTransactionStep,
                runPostPersistStep,
                publishResultStep
        ));
    }
}
