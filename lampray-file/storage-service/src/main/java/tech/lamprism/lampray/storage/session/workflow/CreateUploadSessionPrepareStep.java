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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.session.DirectUploadProvision;
import tech.lamprism.lampray.storage.session.DirectUploadRequestCreator;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class CreateUploadSessionPrepareStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private static final StorageContentRules CONTENT_RULES = StorageContentRules.INSTANCE;

    private final StorageRuntimeConfig runtimeSettings;
    private final StorageGroupRouter storageGroupRouter;
    private final BlobStoreLocator blobStoreLocator;
    private final ResourceIdGenerator resourceIdGenerator;
    private final DirectUploadRequestCreator directUploadRequestCreator;
    private final StorageTransferModeResolver transferModeResolver;

    CreateUploadSessionPrepareStep(StorageRuntimeConfig runtimeSettings,
                                   StorageGroupRouter storageGroupRouter,
                                   BlobStoreLocator blobStoreLocator,
                                   ResourceIdGenerator resourceIdGenerator,
                                   DirectUploadRequestCreator directUploadRequestCreator) {
        this.runtimeSettings = runtimeSettings;
        this.storageGroupRouter = storageGroupRouter;
        this.blobStoreLocator = blobStoreLocator;
        this.resourceIdGenerator = resourceIdGenerator;
        this.directUploadRequestCreator = directUploadRequestCreator;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) throws IOException {
        StorageUploadRequest request = context.getRequest();
        String groupName = resolveGroupName(request.getGroupName());
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
        String mimeType = CONTENT_RULES.requireMimeType(request.getMimeType());
        FileType fileType = CONTENT_RULES.resolveFileType(mimeType);
        StorageUploadSessionModel.validateUploadRequest(request, groupSettings, fileType);
        String contentChecksum = StorageUploadSessionModel.normalizeChecksum(request.getContentChecksum());
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
                normalizedRequest.contentChecksum,
                primaryBlobStore
        );
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.getPendingUploadExpireSeconds());
        DirectUploadState directUploadState = createDirectUploadIfNeeded(
                context.getRequest(),
                groupName,
                primaryBackend,
                normalizedRequest,
                primaryBlobStore,
                uploadMode
        );
        return StorageUploadSessionModel.pending(
                newId(),
                newId(),
                groupName,
                normalizedRequest.fileName,
                context.getRequest().getSize(),
                normalizedRequest.mimeType,
                normalizedRequest.fileType,
                normalizedRequest.contentChecksum,
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
                                                         StorageUploadMode uploadMode) throws IOException {
        if (uploadMode != StorageUploadMode.DIRECT) {
            return DirectUploadState.empty();
        }
        DirectUploadProvision provision = directUploadRequestCreator.create(
                groupName,
                primaryBackend,
                normalizedRequest.mimeType,
                normalizedRequest.contentChecksum,
                Objects.requireNonNull(request.getSize(), "Direct uploads require a declared size."),
                primaryBlobStore,
                runtimeSettings.getDirectAccessTtlSeconds()
        );
        return new DirectUploadState(provision.getAccessRequest(), provision.getObjectKey());
    }

    private String resolveGroupName(String requestedGroupName) {
        String normalizedGroupName = StringUtils.trimToNull(requestedGroupName);
        return normalizedGroupName != null ? normalizedGroupName : runtimeSettings.getDefaultGroup();
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
        private final String contentChecksum;

        private NormalizedUploadRequest(String fileName,
                                        String mimeType,
                                        FileType fileType,
                                        String contentChecksum) {
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
