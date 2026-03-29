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

import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionResolveUploadModeStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreLocator blobStoreLocator;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageTransferModeResolver transferModeResolver;

    CreateUploadSessionResolveUploadModeStep(StorageRuntimeConfig runtimeSettings,
                                             BlobStoreLocator blobStoreLocator,
                                             ResourceIdGenerator resourceIdGenerator) {
        this.runtimeSettings = runtimeSettings;
        this.blobStoreLocator = blobStoreLocator;
        this.resourceIdGenerator = resourceIdGenerator;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        String uploadId = newId();
        String fileId = newId();
        String primaryBackend = Objects.requireNonNull(context.getState().getWritePlan(), "writePlan").getPrimaryBackend();
        BlobStore primaryBlobStore = blobStoreLocator.require(primaryBackend);
        StorageUploadMode uploadMode = transferModeResolver.resolveUploadMode(
                context.getRequest(),
                context.getState().getChecksum(),
                primaryBlobStore
        );
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.getPendingUploadExpireSeconds());

        context.getState().setUploadId(uploadId);
        context.getState().setFileId(fileId);
        context.getState().setPrimaryBackend(primaryBackend);
        context.getState().setPrimaryBlobStore(primaryBlobStore);
        context.getState().setUploadMode(uploadMode);
        context.getState().setNow(now);
        context.getState().setExpiresAt(expiresAt);
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
