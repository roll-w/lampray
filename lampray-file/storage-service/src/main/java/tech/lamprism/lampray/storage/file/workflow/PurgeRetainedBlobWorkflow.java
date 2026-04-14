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
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author RollW
 */
@Component
public class PurgeRetainedBlobWorkflow implements Workflow<PurgeRetainedBlobWorkflowContext, Boolean> {
    private final List<WorkflowStep<PurgeRetainedBlobWorkflowContext>> steps;
    private final LockService lockService;

    public PurgeRetainedBlobWorkflow(List<WorkflowStep<PurgeRetainedBlobWorkflowContext>> steps,
                                     LockService lockService) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
        this.lockService = lockService;
    }

    @Override
    public Boolean execute(PurgeRetainedBlobWorkflowContext context) throws IOException {
        try (LockService.AcquiredLock ignored = lockService.acquire(context.getLockKey())) {
            for (WorkflowStep<PurgeRetainedBlobWorkflowContext> step : steps) {
                step.execute(context);
            }
            return context.getState().getResult();
        }
    }
}
