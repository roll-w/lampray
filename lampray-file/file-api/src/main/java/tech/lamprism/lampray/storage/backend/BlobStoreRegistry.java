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

import tech.lamprism.lampray.storage.store.BlobStore;

import java.util.Collection;
import java.util.Optional;

/**
 * Tracks blob stores that are available at runtime.
 *
 * @author RollW
 */
public interface BlobStoreRegistry extends AutoCloseable {
    public void register(BlobStoreRegistration registration);

    public Optional<BlobStoreRegistration> unregister(String backendName);

    public Optional<BlobStore> find(String backendName);

    public BlobStore get(String backendName);

    public Collection<BlobStoreRegistration> registrations();

    public boolean contains(String backendName);

    public Collection<BlobStore> all();

    @Override
    public void close() throws Exception;
}
