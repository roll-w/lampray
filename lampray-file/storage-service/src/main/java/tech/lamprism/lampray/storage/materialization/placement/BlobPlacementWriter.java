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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes blob placements to storage backends.
 *
 * @author RollW
 */
public interface BlobPlacementWriter {
    /**
     * Uploads a temporary file to a backend.
     */
    void putTempToBackend(String backendName,
                          String objectKey,
                          Path tempPath,
                          long size,
                          String mimeType,
                          String contentChecksum) throws IOException;

    /**
     * Replicates an object from one backend to another.
     */
    void replicateBetweenBackends(String sourceBackend,
                                  String sourceObjectKey,
                                  String targetBackend,
                                  String targetObjectKey,
                                  long size,
                                  String mimeType,
                                  String checksum) throws IOException;
}
