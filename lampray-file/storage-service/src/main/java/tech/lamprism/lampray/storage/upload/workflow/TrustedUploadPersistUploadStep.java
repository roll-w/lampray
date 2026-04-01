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
import tech.lamprism.lampray.storage.file.workflow.PersistTrustedUploadWorkflow;
import tech.lamprism.lampray.storage.file.workflow.PersistTrustedUploadWorkflowContext;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class TrustedUploadPersistUploadStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private final PersistTrustedUploadWorkflow persistTrustedUploadWorkflow;

    TrustedUploadPersistUploadStep(PersistTrustedUploadWorkflow persistTrustedUploadWorkflow) {
        this.persistTrustedUploadWorkflow = persistTrustedUploadWorkflow;
    }

    @Override
    public int getOrder() {
        return 700;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        FileStorage fileStorage = persistTrustedUploadWorkflow.execute(
                new PersistTrustedUploadWorkflowContext(
                        Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                        Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                        null,
                        Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
                )
        );
        context.getState().setResult(fileStorage);
    }
}
