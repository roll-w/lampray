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

package tech.lamprism.lampray.storage.access;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Resolves access for persisted storage files.
 *
 * @author RollW
 */
@Component
public class StoredStorageAccessResolver {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageTransferModeResolver transferModeResolver;
    private final StoredDownloadTargetResolver storedDownloadTargetResolver;
    private final Map<StorageDownloadMode, StoredAccessStrategy> accessStrategies;

    public StoredStorageAccessResolver(StorageRuntimeConfig runtimeSettings,
                                       StoredDownloadTargetResolver storedDownloadTargetResolver,
                                       List<StoredAccessStrategy> accessStrategies) {
        this.runtimeSettings = runtimeSettings;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
        this.storedDownloadTargetResolver = storedDownloadTargetResolver;
        this.accessStrategies = Maps.uniqueIndex(accessStrategies, StoredAccessStrategy::mode);
    }

    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        StoredDownloadTarget target = storedDownloadTargetResolver.resolve(fileId, userId);
        StorageDownloadMode mode = transferModeResolver.resolveDownloadMode(
                target.getFileStorage(),
                target.getGroupConfig(),
                target.getBlobStore()
        );
        StoredAccessStrategy strategy = accessStrategy(mode);
        StorageDownloadResult directOrProxy = strategy.resolveDownload(target);
        if (directOrProxy != null) {
            return directOrProxy;
        }
        return accessStrategy(StorageDownloadMode.PROXY).resolveDownload(target);
    }

    public StorageReference resolveReference(String fileId,
                                             StorageReferenceRequest request,
                                             Long userId) throws IOException {
        StoredDownloadTarget target = storedDownloadTargetResolver.resolve(fileId, userId);
        StorageDownloadMode resolvedMode = transferModeResolver.resolveDownloadMode(
                target.getFileStorage(),
                target.getGroupConfig(),
                target.getBlobStore()
        );
        if (!runtimeSettings.getDirectAccessEnabled()) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw new StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct storage reference is disabled: " + fileId
            );
        }

        if (resolvedMode != StorageDownloadMode.DIRECT) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw new StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct storage reference is not available: " + fileId
            );
        }

        StorageReference directReference;
        try {
            directReference = accessStrategy(StorageDownloadMode.DIRECT).resolveReference(fileId, target, request);
        } catch (IOException exception) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw exception;
        }
        if (directReference != null) {
            return directReference;
        }
        if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
            return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
        }
        throw new StorageException(
                CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Direct storage reference is not available: " + fileId
        );
    }

    public StorageReference proxyReference(String fileId) {
        return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, null, new StorageReferenceRequest());
    }

    private StoredAccessStrategy accessStrategy(StorageDownloadMode mode) {
        StoredAccessStrategy strategy = accessStrategies.get(mode);
        if (strategy == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Unsupported storage access mode: " + mode);
        }
        return strategy;
    }

}
