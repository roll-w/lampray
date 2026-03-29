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

import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.support.PathCleanupSupport;
import tech.lamprism.lampray.storage.workflow.Workflow;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public class ProxyUploadWorkflow implements Workflow<ProxyUploadWorkflowContext, FileStorage> {
    private final List<WorkflowStep<ProxyUploadWorkflowContext>> steps;

    public ProxyUploadWorkflow(List<WorkflowStep<ProxyUploadWorkflowContext>> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public FileStorage execute(ProxyUploadWorkflowContext context) throws IOException {
        try {
            for (WorkflowStep<ProxyUploadWorkflowContext> step : steps) {
                step.execute(context);
            }
            return Objects.requireNonNull(context.getState().getResult(), "proxyUploadResult");
        } finally {
            TempUpload tempUpload = context.getState().getTempUpload();
            if (tempUpload != null) {
                PathCleanupSupport.deleteIfExistsQuietly(tempUpload.getPath());
            }
        }
    }
}
