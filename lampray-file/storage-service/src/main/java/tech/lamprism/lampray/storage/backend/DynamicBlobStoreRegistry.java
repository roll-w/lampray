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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RollW
 */
public class DynamicBlobStoreRegistry implements BlobStoreRegistry {
    private final Map<String, BlobStoreRegistration> registrations = new ConcurrentHashMap<>();
    private final Map<String, BlobStoreRegistration> retiredRegistrations = new ConcurrentHashMap<>();

    public DynamicBlobStoreRegistry(List<BlobStoreRegistration> initialRegistrations) {
        for (BlobStoreRegistration registration : initialRegistrations) {
            register(registration);
        }
    }

    @Override
    public void register(BlobStoreRegistration registration) {
        if (retiredRegistrations.containsKey(registration.getBackendName())) {
            throw new IllegalStateException(
                    "Blob store backend is retired and cannot be re-registered yet: " + registration.getBackendName()
            );
        }
        BlobStoreRegistration previous = registrations.putIfAbsent(registration.getBackendName(), registration);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate blob store backend: " + registration.getBackendName());
        }
    }

    @Override
    public Optional<BlobStoreRegistration> unregister(String backendName) {
        BlobStoreRegistration removed = registrations.remove(backendName);
        if (removed != null) {
            retiredRegistrations.put(backendName, removed);
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public Optional<BlobStore> find(String backendName) {
        BlobStoreRegistration registration = registrations.get(backendName);
        if (registration == null) {
            registration = retiredRegistrations.get(backendName);
        }
        return Optional.ofNullable(registration)
                .map(BlobStoreRegistration::getBlobStore);
    }

    @Override
    public BlobStore get(String backendName) {
        return find(backendName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown blob store backend: " + backendName));
    }

    @Override
    public Collection<BlobStoreRegistration> registrations() {
        return List.copyOf(snapshot().values());
    }

    @Override
    public boolean contains(String backendName) {
        return registrations.containsKey(backendName);
    }

    @Override
    public Collection<BlobStore> all() {
        return registrations().stream()
                .map(BlobStoreRegistration::getBlobStore)
                .toList();
    }

    @Override
    public void close() throws Exception {
        Exception closeFailure = null;
        for (BlobStoreRegistration registration : registrations()) {
            closeFailure = closeAndCollect(registration.getBlobStore(), closeFailure);
        }
        for (BlobStoreRegistration registration : retiredRegistrations.values()) {
            closeFailure = closeAndCollect(registration.getBlobStore(), closeFailure);
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    private Map<String, BlobStoreRegistration> snapshot() {
        return Map.copyOf(new LinkedHashMap<>(registrations));
    }

    private Exception closeAndCollect(BlobStore blobStore,
                                      Exception existingFailure) {
        if (blobStore instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception exception) {
                IllegalStateException failure = new IllegalStateException(
                        "Failed to close blob store backend: " + blobStore.getBackendName(),
                        exception
                );
                if (existingFailure == null) {
                    return failure;
                }
                existingFailure.addSuppressed(failure);
            }
        }
        return existingFailure;
    }
}
