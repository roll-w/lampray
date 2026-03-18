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

package tech.lamprism.lampray.storage.awss3;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreFactory;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class S3BlobStoreFactory implements BlobStoreFactory {
    @Override
    public StorageBackendType getBackendType() {
        return StorageBackendType.S3;
    }

    @Override
    public BlobStore create(StorageBackendConfig config) {
        return new S3BlobStore(
                config.getName(),
                config.getEndpoint(),
                config.getPublicEndpoint(),
                config.getNativeChecksumEnabled(),
                config.getRegion(),
                Objects.requireNonNull(config.getBucket()),
                config.getRootPrefix(),
                config.getPathStyleAccess(),
                config.getAccessKey(),
                config.getSecretKey()
        );
    }
}
