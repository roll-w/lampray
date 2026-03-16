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

package tech.lamprism.lampray.storage.store;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
public class DefaultBlobStoreRegistry implements BlobStoreRegistry {
    private final Map<String, BlobStore> blobStores;

    public DefaultBlobStoreRegistry(List<BlobStore> blobStores) {
        Map<String, BlobStore> mapping = new LinkedHashMap<>();
        for (BlobStore blobStore : blobStores) {
            BlobStore previous = mapping.put(blobStore.getBackendName(), blobStore);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate blob store backend: " + blobStore.getBackendName());
            }
        }
        this.blobStores = Map.copyOf(mapping);
    }

    @Override
    public BlobStore get(String backendName) {
        BlobStore blobStore = blobStores.get(backendName);
        if (blobStore == null) {
            throw new IllegalArgumentException("Unknown blob store backend: " + backendName);
        }
        return blobStore;
    }

    @Override
    public boolean contains(String backendName) {
        return blobStores.containsKey(backendName);
    }

    @Override
    public Collection<BlobStore> all() {
        return blobStores.values();
    }

    @Override
    public void close() throws Exception {
        for (BlobStore blobStore : blobStores.values()) {
            if (blobStore instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }
}
