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
import tech.lamprism.lampray.storage.awss3.S3BlobStore;
import tech.lamprism.lampray.storage.local.LocalBlobStore;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.storage.store.DefaultBlobStoreRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public BlobStoreRegistry blobStoreRegistry(StorageTopology storageTopology) throws IOException {
        List<BlobStore> blobStores = new ArrayList<>();
        for (StorageBackendSettings backendSettings : storageTopology.getBackends().values()) {
            if (backendSettings.getType() == StorageBackendType.LOCAL) {
                blobStores.add(new LocalBlobStore(
                        backendSettings.getName(),
                        Path.of(Objects.requireNonNull(backendSettings.getRootPath())),
                        backendSettings.getRootPrefix()
                ));
                continue;
            }
            blobStores.add(new S3BlobStore(
                    backendSettings.getName(),
                    backendSettings.getEndpoint(),
                    backendSettings.getRegion(),
                    backendSettings.getBucket(),
                    backendSettings.getRootPrefix(),
                    backendSettings.getPathStyleAccess(),
                    backendSettings.getAccessKey(),
                    backendSettings.getSecretKey()
            ));
        }
        return new DefaultBlobStoreRegistry(blobStores);
    }
}
