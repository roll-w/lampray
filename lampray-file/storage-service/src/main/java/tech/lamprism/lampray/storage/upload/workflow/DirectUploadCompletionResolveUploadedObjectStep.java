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

import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class DirectUploadCompletionResolveUploadedObjectStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final BlobStoreLocator blobStoreLocator;

    DirectUploadCompletionResolveUploadedObjectStep(BlobStoreLocator blobStoreLocator) {
        this.blobStoreLocator = blobStoreLocator;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore primaryBlobStore = blobStoreLocator.require(context.getUploadSession().getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(context.getUploadSession().getObjectKey()));
        context.getState().setPrimaryBlobStore(primaryBlobStore);
        context.getState().setUploadedObject(uploadedObject);
    }
}
