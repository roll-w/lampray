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
public class PersistSessionUploadWorkflowFactory {
    private final WorkflowStep<PersistSessionUploadWorkflowContext> persistInTransactionStep;
    private final WorkflowStep<PersistSessionUploadWorkflowContext> runPostPersistStep;
    private final WorkflowStep<PersistSessionUploadWorkflowContext> publishResultStep;

    public PersistSessionUploadWorkflowFactory(StorageFilePersistenceTransactionService storageFilePersistenceTransactionService,
                                               StorageFilePersistencePostPersistService storageFilePersistencePostPersistService) {
        this.persistInTransactionStep = new PersistSessionUploadPersistInTransactionStep(storageFilePersistenceTransactionService);
        this.runPostPersistStep = new PersistSessionUploadRunPostPersistStep(storageFilePersistencePostPersistService);
        this.publishResultStep = new PersistSessionUploadPublishResultStep();
    }

    public PersistSessionUploadWorkflow create() {
        return new PersistSessionUploadWorkflow(List.of(
                persistInTransactionStep,
                runPostPersistStep,
                publishResultStep
        ));
    }
}
