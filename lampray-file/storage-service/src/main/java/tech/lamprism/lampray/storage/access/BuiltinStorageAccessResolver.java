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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageReferenceSource;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageRegistry;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageResource;
import tech.lamprism.lampray.web.ExternalEndpointProvider;
import tech.rollw.common.web.CommonErrorCode;

import java.util.Map;

@Component
class BuiltinStorageAccessResolver {
    private final BuiltinStorageRegistry builtinStorageRegistry;
    private final ExternalEndpointProvider externalEndpointProvider;

    BuiltinStorageAccessResolver(BuiltinStorageRegistry builtinStorageRegistry,
                                 ExternalEndpointProvider externalEndpointProvider) {
        this.builtinStorageRegistry = builtinStorageRegistry;
        this.externalEndpointProvider = externalEndpointProvider;
    }

    StorageDownloadResult resolveDownload(String fileId) {
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

    StorageReference resolveReference(String fileId,
                                      StorageReferenceRequest request) {
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

    StorageReference proxyReference(String fileId) {
        return new StorageReference(
                joinUrl(externalEndpointProvider.getExternalApiEndpoint(), "/api/v1/files/" + fileId),
                StorageDownloadMode.PROXY,
                StorageReferenceSource.API,
                Map.of(),
                null
        );
    }

    private BuiltinStorageResource findBuiltinResource(String fileId) {
        if (!builtinStorageRegistry.contains(fileId)) {
            return null;
        }
        return builtinStorageRegistry.get(fileId);
    }

    private String joinUrl(String endpoint,
                           String path) {
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedEndpoint + normalizedPath;
    }
}
