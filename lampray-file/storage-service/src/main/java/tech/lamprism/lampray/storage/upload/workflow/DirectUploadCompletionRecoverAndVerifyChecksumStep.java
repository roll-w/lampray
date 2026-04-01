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

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionRecoverAndVerifyChecksumStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final int BUFFER_SIZE = 8192;

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore blobStore = Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore");
        BlobObject uploadedObject = Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject");
        String actualChecksum = uploadedObject.getChecksumSha256();
        if (!StringUtils.hasText(actualChecksum)) {
            String metadataChecksum = BlobMetadataSupport.metadataChecksum(uploadedObject.getMetadata());
            if (StringUtils.hasText(metadataChecksum)) {
                actualChecksum = StorageUploadSessionModel.normalizeChecksum(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.getKey());
        }
        StorageUploadSessionModel.validateChecksumMatch(
                Objects.requireNonNull(context.getState().getExpectedChecksum(), "expectedChecksum"),
                actualChecksum
        );
        context.getState().setActualChecksum(actualChecksum);
    }

    private String calculateChecksum(BlobStore blobStore,
                                     String objectKey) throws IOException {
        Hasher hasher = Hashing.sha256().newHasher();
        try (InputStream inputStream = blobStore.openDownload(objectKey).openStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                hasher.putBytes(buffer, 0, read);
            }
        }
        return hasher.hash().toString();
    }
}
