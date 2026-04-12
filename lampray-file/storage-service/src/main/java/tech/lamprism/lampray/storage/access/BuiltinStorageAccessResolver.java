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

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageRegistry;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageResource;
import tech.rollw.common.web.CommonErrorCode;

/**
 * Resolves access for builtin storage resources.
 *
 * @author RollW
 */
@Component
@Order(0)
public class BuiltinStorageAccessResolver implements StorageAccessResolver {
    private final BuiltinStorageRegistry builtinStorageRegistry;
    private final ProxyStorageReferenceFactory proxyStorageReferenceFactory;

    public BuiltinStorageAccessResolver(BuiltinStorageRegistry builtinStorageRegistry,
                                        ProxyStorageReferenceFactory proxyStorageReferenceFactory) {
        this.builtinStorageRegistry = builtinStorageRegistry;
        this.proxyStorageReferenceFactory = proxyStorageReferenceFactory;
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) {
        BuiltinStorageResource builtinResource = findBuiltinResource(fileId);
        if (builtinResource == null) {
            return null;
        }
        return new StorageDownloadResult(
                builtinResource.getFileStorage(),
                StorageDownloadMode.PROXY,
                null,
                builtinResource.getContent()
        );
    }

    @Override
    public StorageReference resolveReference(String fileId,
                                             StorageReferenceRequest request,
                                             Long userId) {
        BuiltinStorageResource builtinResource = findBuiltinResource(fileId);
        if (builtinResource == null) {
            return null;
        }
        if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
            return proxyReference(fileId);
        }
        throw new StorageException(
                CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Direct storage reference is not available: " + fileId
        );
    }

    public StorageReference proxyReference(String fileId) {
        return proxyStorageReferenceFactory.create(fileId);
    }

    private BuiltinStorageResource findBuiltinResource(String fileId) {
        if (!builtinStorageRegistry.contains(fileId)) {
            return null;
        }
        return builtinStorageRegistry.get(fileId);
    }
}
