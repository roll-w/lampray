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
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionBuildResultStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        context.getState().setResult(new StorageUploadSession(
                Objects.requireNonNull(context.getState().getUploadId(), "uploadId"),
                Objects.requireNonNull(context.getState().getUploadMode(), "uploadMode"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileId(), "fileId"),
                context.getState().getDirectRequest(),
                Objects.requireNonNull(context.getState().getExpiresAt(), "expiresAt")
        ));
    }
}
