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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.session.DirectUploadProvision;
import tech.lamprism.lampray.storage.session.DirectUploadRequestCreator;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class CreateUploadSessionCreateDirectUploadIfNeededStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final DirectUploadRequestCreator directUploadRequestCreator;

    CreateUploadSessionCreateDirectUploadIfNeededStep(StorageRuntimeConfig runtimeSettings,
                                                      DirectUploadRequestCreator directUploadRequestCreator) {
        this.runtimeSettings = runtimeSettings;
        this.directUploadRequestCreator = directUploadRequestCreator;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) throws IOException {
        if (context.getState().getUploadMode() != StorageUploadMode.DIRECT) {
            return;
        }
        long declaredSize = Objects.requireNonNull(context.getRequest().getSize(), "Direct uploads require a declared size.");
        DirectUploadProvision provision = directUploadRequestCreator.create(
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getPrimaryBackend(), "primaryBackend"),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                context.getState().getChecksum(),
                declaredSize,
                Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore"),
                runtimeSettings.getDirectAccessTtlSeconds()
        );
        context.getState().setObjectKey(provision.getObjectKey());
        context.getState().setDirectRequest(provision.getAccessRequest());
    }
}
