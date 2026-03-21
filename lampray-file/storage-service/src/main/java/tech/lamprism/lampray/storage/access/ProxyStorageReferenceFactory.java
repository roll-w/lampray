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
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceSource;
import tech.lamprism.lampray.web.ExternalEndpointProvider;

import java.util.Map;

@Component
class ProxyStorageReferenceFactory {
    private final ExternalEndpointProvider externalEndpointProvider;

    ProxyStorageReferenceFactory(ExternalEndpointProvider externalEndpointProvider) {
        this.externalEndpointProvider = externalEndpointProvider;
    }

    StorageReference create(String fileId) {
        return new StorageReference(
                joinUrl(externalEndpointProvider.getExternalApiEndpoint(), "/api/v1/files/" + fileId),
                StorageDownloadMode.PROXY,
                StorageReferenceSource.API,
                Map.of(),
                null
        );
    }

    private String joinUrl(String endpoint,
                           String path) {
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedEndpoint + normalizedPath;
    }
}
