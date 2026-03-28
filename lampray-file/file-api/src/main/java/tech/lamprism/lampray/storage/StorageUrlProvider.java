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

package tech.lamprism.lampray.storage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Resolves proxy or direct references for stored content.
 *
 * @author RollW
 */
public interface StorageUrlProvider {
    /**
     * Resolves a storage reference using the requested access mode.
     */
    StorageReference resolveStorageReference(String id,
                                             StorageReferenceRequest request,
                                             Long userId) throws IOException;

    /**
     * Returns the default proxy URL for a storage id.
     */
    default String getUrlOfStorage(String id) {
        try {
            return resolveStorageReference(id, new StorageReferenceRequest(), null).getUrl();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to resolve storage url: " + id, exception);
        }
    }
}
