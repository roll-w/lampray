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
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.checksum.ContentFingerprint;
import tech.lamprism.lampray.storage.checksum.ContentFingerprintProfile;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class CreateUploadSessionPrepareStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {

    private final StorageRuntimeConfig runtimeSettings;
    private final StorageGroupRouter storageGroupRouter;
    private final BlobStoreLocator blobStoreLocator;
    private final ResourceIdGenerator resourceIdGenerator;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final StorageTrafficPublisher storageTrafficPublisher;
    private final StorageTransferModeResolver transferModeResolver;
    private final ContentFingerprintProfile contentFingerprintProfile;

    CreateUploadSessionPrepareStep(StorageRuntimeConfig runtimeSettings,
                                   StorageGroupRouter storageGroupRouter,
                                    BlobStoreLocator blobStoreLocator,
                                    ResourceIdGenerator resourceIdGenerator,
                                    BlobObjectKeyFactory blobObjectKeyFactory,
                                    StorageTrafficPublisher storageTrafficPublisher,
                                    ContentFingerprintProfile contentFingerprintProfile) {
        this.runtimeSettings = runtimeSettings;
        this.storageGroupRouter = storageGroupRouter;
        this.blobStoreLocator = blobStoreLocator;
        this.resourceIdGenerator = resourceIdGenerator;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.storageTrafficPublisher = storageTrafficPublisher;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
        this.contentFingerprintProfile = contentFingerprintProfile;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) throws IOException {
        StorageUploadRequest request = context.getRequest();
        String groupName = resolveGroupName();
        StorageWritePlan writePlan = selectWritePlan(groupName);
        NormalizedUploadRequest normalizedRequest = normalizeRequest(request, writePlan.getGroupSettings());
        StorageUploadSessionModel uploadSession = prepareUploadSession(
                context,
                groupName,
                writePlan,
                normalizedRequest
        );
        context.getState().setUploadSession(uploadSession);
    }

    private NormalizedUploadRequest normalizeRequest(StorageUploadRequest request,
                                                     StorageGroupConfig groupSettings) {
        String fileName = StorageUploadSessionModel.normalizeFileName(request.getFileName());
        String mimeType = StorageContentRules.requireMimeType(request.getMimeType());
        FileType fileType = StorageContentRules.resolveFileType(mimeType);
        StorageUploadSessionModel.validateUploadRequest(request, groupSettings, fileType);
        String normalizedChecksum = StorageUploadSessionModel.normalizeChecksum(request.getContentChecksum(), contentFingerprintProfile);
        ContentFingerprint contentChecksum = normalizedChecksum != null
                ? ContentFingerprint.parse(normalizedChecksum, contentFingerprintProfile)
                : null;
        return new NormalizedUploadRequest(fileName, mimeType, fileType, contentChecksum);
    }

    private StorageUploadSessionModel prepareUploadSession(CreateUploadSessionWorkflowContext context,
                                                           String groupName,
                                                           StorageWritePlan writePlan,
                                                            NormalizedUploadRequest normalizedRequest) throws IOException {
        String primaryBackend = writePlan.getPrimaryBackend();
        BlobStore primaryBlobStore = blobStoreLocator.require(primaryBackend);
        StorageUploadMode uploadMode = transferModeResolver.resolveUploadMode(
                context.getRequest(),
                normalizedRequest.contentChecksum != null ? normalizedRequest.contentChecksum.encoded() : null,
                primaryBlobStore
        );
        String uploadId = newId();
        String fileId = newId();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.getPendingUploadExpireSeconds());
        DirectUploadState directUploadState = createDirectUploadIfNeeded(
                context.getRequest(),
                groupName,
                primaryBackend,
                normalizedRequest,
                primaryBlobStore,
                uploadMode,
                uploadId
        );
        return StorageUploadSessionModel.pending(
                uploadId,
                fileId,
                groupName,
                normalizedRequest.fileName,
                context.getRequest().getSize(),
                normalizedRequest.mimeType,
                normalizedRequest.fileType,
                normalizedRequest.contentChecksum != null ? normalizedRequest.contentChecksum.encoded() : null,
                context.getUserId(),
                primaryBackend,
                directUploadState.objectKey,
                uploadMode,
                expiresAt,
                now,
                directUploadState.directRequest
        );
    }

    private DirectUploadState createDirectUploadIfNeeded(StorageUploadRequest request,
                                                         String groupName,
                                                         String primaryBackend,
                                                         NormalizedUploadRequest normalizedRequest,
                                                         BlobStore primaryBlobStore,
                                                         StorageUploadMode uploadMode,
                                                         String objectKeySeed) throws IOException {
        if (uploadMode != StorageUploadMode.DIRECT) {
            return DirectUploadState.empty();
        }
        long declaredSize = Objects.requireNonNull(request.getSize(), "Direct uploads require a declared size.");
        String objectKey = blobObjectKeyFactory.createKey(
                Objects.requireNonNull(normalizedRequest.contentChecksum).encoded(),
                objectKeySeed
        );
        StorageAccessRequest accessRequest = primaryBlobStore.createDirectUpload(
                new BlobWriteRequest(
                        objectKey,
                        declaredSize,
                        normalizedRequest.mimeType,
                        BlobMetadataSupport.contentFingerprintMetadata(
                                normalizedRequest.contentChecksum.encoded(),
                                contentFingerprintProfile
                        ),
                        normalizedRequest.contentChecksum.primaryChecksum()
                ),
                Duration.ofSeconds(runtimeSettings.getDirectAccessTtlSeconds())
        );
        storageTrafficPublisher.publishDirectUploadRequest(groupName, primaryBackend, declaredSize);
        return new DirectUploadState(accessRequest, objectKey);
    }

    private String resolveGroupName() {
        return runtimeSettings.getDefaultGroup();
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }

    private StorageWritePlan selectWritePlan(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, exception.getMessage());
        }
    }

    private static final class NormalizedUploadRequest {
        private final String fileName;
        private final String mimeType;
        private final FileType fileType;
        private final ContentFingerprint contentChecksum;

        private NormalizedUploadRequest(String fileName,
                                        String mimeType,
                                        FileType fileType,
                                        ContentFingerprint contentChecksum) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileType = fileType;
            this.contentChecksum = contentChecksum;
        }
    }

    private static final class DirectUploadState {
        private final StorageAccessRequest directRequest;
        private final String objectKey;

        private DirectUploadState(StorageAccessRequest directRequest,
                                  String objectKey) {
            this.directRequest = directRequest;
            this.objectKey = objectKey;
        }

        private static DirectUploadState empty() {
            return new DirectUploadState(null, null);
        }
    }
}
