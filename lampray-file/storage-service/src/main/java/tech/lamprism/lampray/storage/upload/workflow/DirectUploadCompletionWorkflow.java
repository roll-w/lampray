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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.support.StorageBlobLifecycleLockManager;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class DirectUploadCompletionWorkflow implements Workflow<DirectUploadCompletionWorkflowContext, FileStorage> {
    private final List<WorkflowStep<DirectUploadCompletionWorkflowContext>> steps;
    private final StorageBlobLifecycleLockManager storageBlobLifecycleLockManager;

    public DirectUploadCompletionWorkflow(List<WorkflowStep<DirectUploadCompletionWorkflowContext>> steps,
                                          StorageBlobLifecycleLockManager storageBlobLifecycleLockManager) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
        this.storageBlobLifecycleLockManager = storageBlobLifecycleLockManager;
    }

    @Override
    public FileStorage execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        try (StorageBlobLifecycleLockManager.LockedKey ignored = storageBlobLifecycleLockManager.acquire(
                context.getUploadSession().requireChecksum()
        )) {
            for (WorkflowStep<DirectUploadCompletionWorkflowContext> step : steps) {
                step.execute(context);
            }
            return Objects.requireNonNull(context.getState().getResult(), "directUploadCompletionResult");
        }
    }
}
