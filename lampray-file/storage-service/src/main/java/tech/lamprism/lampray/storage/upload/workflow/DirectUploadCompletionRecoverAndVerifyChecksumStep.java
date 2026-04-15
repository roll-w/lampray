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
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.checksum.ContentFingerprint;
import tech.lamprism.lampray.storage.checksum.ContentFingerprintHasher;
import tech.lamprism.lampray.storage.checksum.ContentFingerprintProfile;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class DirectUploadCompletionRecoverAndVerifyChecksumStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final int BUFFER_SIZE = 8192;

    private final ContentFingerprintProfile contentFingerprintProfile;

    DirectUploadCompletionRecoverAndVerifyChecksumStep(ContentFingerprintProfile contentFingerprintProfile) {
        this.contentFingerprintProfile = contentFingerprintProfile;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore blobStore = Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore");
        BlobObject uploadedObject = Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject");
        ContentFingerprint actualChecksum = resolveFingerprint(blobStore, uploadedObject);
        StorageUploadSessionModel.validateChecksumMatch(
                Objects.requireNonNull(context.getState().getExpectedChecksum(), "expectedChecksum"),
                actualChecksum.encoded(),
                contentFingerprintProfile
        );
        context.getState().setActualChecksum(actualChecksum.encoded());
    }

    private ContentFingerprint resolveFingerprint(BlobStore blobStore,
                                                  BlobObject uploadedObject) throws IOException {
        String primaryChecksum = resolvePrimaryChecksum(uploadedObject);
        ContentFingerprint actualFingerprint = calculateFingerprint(blobStore, uploadedObject.getKey(), primaryChecksum);
        String metadataContentFingerprint = BlobMetadataSupport.metadataContentFingerprint(
                uploadedObject.getMetadata(),
                contentFingerprintProfile
        );
        if (StringUtils.hasText(metadataContentFingerprint)) {
            ContentFingerprint declaredFingerprint = parseFingerprint(metadataContentFingerprint);
            if (!actualFingerprint.equals(declaredFingerprint)) {
                throw new StorageException(
                        CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                        "Uploaded file fingerprint metadata does not match uploaded content."
                );
            }
        }
        return actualFingerprint;
    }

    private String resolvePrimaryChecksum(BlobObject uploadedObject) {
        String primaryChecksum = uploadedObject.getContentChecksum();
        if (!StringUtils.hasText(primaryChecksum)) {
            primaryChecksum = BlobMetadataSupport.metadataContentChecksum(uploadedObject.getMetadata(), contentFingerprintProfile);
        }
        return StringUtils.hasText(primaryChecksum)
                ? ContentFingerprint.normalizePrimaryChecksum(primaryChecksum, contentFingerprintProfile)
                : null;
    }

    private ContentFingerprint calculateFingerprint(BlobStore blobStore,
                                                    String objectKey,
                                                    String primaryChecksum) throws IOException {
        ContentFingerprintHasher hasher = StringUtils.hasText(primaryChecksum)
                ? ContentFingerprintHasher.forKnownPrimaryChecksum(primaryChecksum, contentFingerprintProfile)
                : ContentFingerprintHasher.create(contentFingerprintProfile);
        try (InputStream inputStream = blobStore.openDownload(objectKey).openStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                hasher.putBytes(buffer, 0, read);
            }
        }
        return hasher.finish();
    }

    private ContentFingerprint parseFingerprint(String rawValue) {
        try {
            return ContentFingerprint.parse(rawValue, contentFingerprintProfile);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        }
    }
}
