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

package tech.lamprism.lampray.storage.backend;

import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.store.BlobStoreFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapBackedBlobStoreFactoryProvider implements BlobStoreFactoryProvider {
    private final Map<StorageBackendType, BlobStoreFactory> factoriesByType;

    public MapBackedBlobStoreFactoryProvider(List<BlobStoreFactory> blobStoreFactories) {
        Map<StorageBackendType, BlobStoreFactory> resolvedFactories = new LinkedHashMap<>();
        for (BlobStoreFactory blobStoreFactory : blobStoreFactories) {
            BlobStoreFactory previous = resolvedFactories.put(blobStoreFactory.getBackendType(), blobStoreFactory);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate blob store factory for backend type: " + blobStoreFactory.getBackendType()
                );
            }
        }
        this.factoriesByType = Map.copyOf(resolvedFactories);
    }

    @Override
    public BlobStoreFactory requireFactory(StorageBackendType backendType) {
        BlobStoreFactory blobStoreFactory = factoriesByType.get(backendType);
        if (blobStoreFactory == null) {
            throw new IllegalArgumentException("No blob store factory available for backend type: " + backendType);
        }
        return blobStoreFactory;
    }
}
