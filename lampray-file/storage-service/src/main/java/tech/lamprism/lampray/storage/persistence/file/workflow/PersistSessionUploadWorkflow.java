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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PersistSessionUploadWorkflow implements Workflow<PersistSessionUploadWorkflowContext, FileStorage> {
    private final List<WorkflowStep<PersistSessionUploadWorkflowContext>> steps;

    public PersistSessionUploadWorkflow(List<WorkflowStep<PersistSessionUploadWorkflowContext>> steps) {
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getOrder))
                .toList();
    }

    @Override
    public FileStorage execute(PersistSessionUploadWorkflowContext context) {
        for (WorkflowStep<PersistSessionUploadWorkflowContext> step : steps) {
            try {
                step.execute(context);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Unexpected I/O failure in file persistence workflow", exception);
            }
        }
        return Objects.requireNonNull(context.getState().getResult(), "persistSessionUploadResult");
    }
}
