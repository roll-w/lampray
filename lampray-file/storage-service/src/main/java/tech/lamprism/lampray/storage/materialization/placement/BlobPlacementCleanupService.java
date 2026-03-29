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

package tech.lamprism.lampray.storage.materialization.placement;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class BlobPlacementCleanupService {
    private final BlobStoreLocator blobStoreLocator;

    public BlobPlacementCleanupService(BlobStoreLocator blobStoreLocator) {
        this.blobStoreLocator = blobStoreLocator;
    }

    public void cleanup(Map<String, String> placements,
                        String protectedBackend,
                        String protectedObjectKey) {
        for (Map.Entry<String, String> entry : placements.entrySet()) {
            boolean isProtected = protectedBackend != null
                    && protectedBackend.equals(entry.getKey())
                    && Objects.equals(protectedObjectKey, entry.getValue());
            if (isProtected) {
                continue;
            }
            try {
                blobStoreLocator.require(entry.getKey()).delete(entry.getValue());
            } catch (IOException ignored) {
            }
        }
    }
}
