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

import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionNormalizeAndValidateRequestStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private static final StorageContentRules CONTENT_RULES = StorageContentRules.INSTANCE;
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        StorageUploadRequest request = context.getRequest();
        StorageGroupConfig groupSettings = Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings");
        String fileName = VALIDATION_RULES.normalizeFileName(request.getFileName());
        String mimeType = CONTENT_RULES.requireMimeType(request.getMimeType());
        FileType fileType = CONTENT_RULES.resolveFileType(mimeType);
        VALIDATION_RULES.validateUploadRequest(request, groupSettings, fileType);

        context.getState().setFileName(fileName);
        context.getState().setMimeType(mimeType);
        context.getState().setFileType(fileType);
        context.getState().setChecksum(VALIDATION_RULES.normalizeChecksum(request.getChecksumSha256()));
    }
}
