/*
 * Copyright (C) 2023-2025 RollW
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

package tech.lamprism.lampray.storage.service;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageUrlProviderStrategy;
import tech.lamprism.lampray.web.ExternalEndpointProvider;

/**
 * @author RollW
 */
@Service
public class DefaultStorageUrlProviderStrategy implements StorageUrlProviderStrategy {
    private final ExternalEndpointProvider externalEndpointProvider;

    public DefaultStorageUrlProviderStrategy(ExternalEndpointProvider externalEndpointProvider) {
        this.externalEndpointProvider = externalEndpointProvider;
    }

    @Override
    public String getUrlOfStorage(String id) {
        String externalApiEndpoint = externalEndpointProvider.getExternalApiEndpoint();
        return externalApiEndpoint + "/api/v1/storage/" + id;
    }
}
