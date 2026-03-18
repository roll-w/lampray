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

package tech.lamprism.lampray.storage.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.store.BlobStoreFactory;
import tech.lamprism.lampray.storage.store.BlobStoreRegistration;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.storage.store.DynamicBlobStoreRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Configuration
public class FileStoreConfiguration {
    @Bean
    public StorageTopology storageTopology(StorageTopologyResolver storageTopologyResolver) {
        return storageTopologyResolver.resolve();
    }

    @Bean(destroyMethod = "close")
    public BlobStoreRegistry blobStoreRegistry(StorageTopology storageTopology,
                                               List<BlobStoreFactory> blobStoreFactories) throws IOException {
        Map<StorageBackendType, BlobStoreFactory> factoriesByType = indexFactoriesByType(blobStoreFactories);
        List<BlobStoreRegistration> registrations = new ArrayList<>();
        for (StorageBackendConfig backendConfig : storageTopology.getBackends().values()) {
            BlobStoreFactory blobStoreFactory = requireFactory(factoriesByType, backendConfig.getType());
            registrations.add(new BlobStoreRegistration(blobStoreFactory.create(backendConfig), Map.of()));
        }
        return new DynamicBlobStoreRegistry(registrations);
    }

    private Map<StorageBackendType, BlobStoreFactory> indexFactoriesByType(List<BlobStoreFactory> blobStoreFactories) {
        Map<StorageBackendType, BlobStoreFactory> factoriesByType = new LinkedHashMap<>();
        for (BlobStoreFactory blobStoreFactory : blobStoreFactories) {
            BlobStoreFactory previous = factoriesByType.put(blobStoreFactory.getBackendType(), blobStoreFactory);
            if (previous != null) {
                throw new IllegalStateException("Duplicate blob store factory for backend type: "
                        + blobStoreFactory.getBackendType());
            }
        }
        return factoriesByType;
    }

    private BlobStoreFactory requireFactory(Map<StorageBackendType, BlobStoreFactory> factoriesByType,
                                            StorageBackendType backendType) {
        BlobStoreFactory blobStoreFactory = factoriesByType.get(backendType);
        if (blobStoreFactory == null) {
            throw new IllegalArgumentException("No blob store factory available for backend type: " + backendType);
        }
        return blobStoreFactory;
    }
}
