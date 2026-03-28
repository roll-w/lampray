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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;

import java.io.IOException;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class CompositeStorageAccessService implements StorageAccessService {
    private final BuiltinStorageAccessResolver builtinStorageAccessResolver;
    private final StoredStorageAccessResolver storedStorageAccessResolver;

    public CompositeStorageAccessService(BuiltinStorageAccessResolver builtinStorageAccessResolver,
                                         StoredStorageAccessResolver storedStorageAccessResolver) {
        this.builtinStorageAccessResolver = builtinStorageAccessResolver;
        this.storedStorageAccessResolver = storedStorageAccessResolver;
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        StorageDownloadResult builtinResult = builtinStorageAccessResolver.resolveDownload(fileId);
        if (builtinResult != null) {
            return builtinResult;
        }
        return storedStorageAccessResolver.resolveDownload(fileId, userId);
    }

    @Override
    public StorageReference resolveStorageReference(String id,
                                                     StorageReferenceRequest request,
                                                     Long userId) throws IOException {
        StorageReferenceRequest normalizedRequest = request != null ? request : new StorageReferenceRequest();
        if (normalizedRequest.getMode() == tech.lamprism.lampray.storage.StorageReferenceMode.PROXY) {
            return storedStorageAccessResolver.proxyReference(id);
        }

        StorageReference builtinReference = builtinStorageAccessResolver.resolveReference(id, normalizedRequest);
        if (builtinReference != null) {
            return builtinReference;
        }
        return storedStorageAccessResolver.resolveReference(id, normalizedRequest, userId);
    }
}
