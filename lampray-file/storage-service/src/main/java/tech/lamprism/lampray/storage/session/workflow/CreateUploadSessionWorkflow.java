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

package tech.lamprism.lampray.storage.session.workflow;

import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public class CreateUploadSessionWorkflow implements Workflow<CreateUploadSessionWorkflowContext, StorageUploadSession> {
    private final List<WorkflowStep<CreateUploadSessionWorkflowContext>> steps;

    public CreateUploadSessionWorkflow(List<WorkflowStep<CreateUploadSessionWorkflowContext>> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public StorageUploadSession execute(CreateUploadSessionWorkflowContext context) throws IOException {
        for (WorkflowStep<CreateUploadSessionWorkflowContext> step : steps) {
            step.execute(context);
        }
        return Objects.requireNonNull(context.getState().getResult(), "createUploadSessionResult");
    }
}
