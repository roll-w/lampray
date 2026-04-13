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

package tech.lamprism.lampray.storage.access.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.access.StorageAccessResolver;
import tech.lamprism.lampray.storage.access.StorageAccessService;
import tech.lamprism.lampray.storage.access.reference.ProxyStorageReferenceFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class CompositeStorageAccessService implements StorageAccessService {
    private final List<StorageAccessResolver> storageAccessResolvers;
    private final ProxyStorageReferenceFactory proxyStorageReferenceFactory;

    public CompositeStorageAccessService(List<StorageAccessResolver> storageAccessResolvers,
                                         ProxyStorageReferenceFactory proxyStorageReferenceFactory) {
        this.storageAccessResolvers = storageAccessResolvers;
        this.proxyStorageReferenceFactory = proxyStorageReferenceFactory;
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        for (StorageAccessResolver resolver : storageAccessResolvers) {
            StorageDownloadResult resolved = resolver.resolveDownload(fileId, userId);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new IllegalStateException("No storage access resolver available for: " + fileId);
    }

    @Override
    public StorageReference resolveStorageReference(String id,
                                                    StorageReferenceRequest request,
                                                    Long userId) throws IOException {
        StorageReferenceRequest normalizedRequest = request;
        if (normalizedRequest == null) {
            normalizedRequest = new StorageReferenceRequest();
        }
        if (normalizedRequest.getMode() == StorageReferenceMode.PROXY) {
            return proxyStorageReferenceFactory.create(id);
        }

        for (StorageAccessResolver resolver : storageAccessResolvers) {
            StorageReference resolved = resolver.resolveReference(id, normalizedRequest, userId);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new IllegalStateException("No storage access resolver available for: " + id);
    }
}
