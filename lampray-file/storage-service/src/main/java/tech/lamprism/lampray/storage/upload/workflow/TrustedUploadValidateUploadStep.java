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
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class TrustedUploadValidateUploadStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        StorageUploadSessionModel.validateUploadRequest(
                new StorageUploadRequest(
                        Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                        Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                        tempUpload.getSize(),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        tempUpload.getContentChecksum()
                ),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType")
        );
    }
}
