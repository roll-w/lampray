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

package tech.lamprism.lampray.storage.file.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.lock.LockService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author RollW
 */
@Component
public class DeleteFileWorkflow implements Workflow<DeleteFileWorkflowContext, DeleteFileWorkflowState> {
    private final List<WorkflowStep<DeleteFileWorkflowContext>> steps;
    private final LockService lockService;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;

    public DeleteFileWorkflow(List<WorkflowStep<DeleteFileWorkflowContext>> steps,
                              LockService lockService,
                              StorageFileRepository storageFileRepository,
                              StorageBlobRepository storageBlobRepository) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
        this.lockService = lockService;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
    }

    @Override
    public DeleteFileWorkflowState execute(DeleteFileWorkflowContext context) throws IOException {
        try (LockService.AcquiredLock ignored = lockService.acquire(resolveLockKey(context.getFileId()))) {
            for (WorkflowStep<DeleteFileWorkflowContext> step : steps) {
                step.execute(context);
            }
            return context.getState();
        }
    }

    private String resolveLockKey(String fileId) {
        return storageFileRepository.findActiveById(fileId)
                .flatMap(fileEntity -> storageBlobRepository.findById(fileEntity.getBlobId())
                        .map(StorageBlobEntity::getContentChecksum)
                        .or(() -> java.util.Optional.of(fileEntity.getBlobId())))
                .orElse(fileId);
    }
}
