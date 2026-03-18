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
import java.io.InputStream;

/**
 * Upload operations exposed by the storage module.
 *
 * @author RollW
 */
public interface StorageUploadProvider {
    /**
     * Stores content through the default upload flow.
     */
    FileStorage saveFile(InputStream inputStream) throws IOException;

    /**
     * Creates an upload session for proxy or direct uploads.
     */
    StorageUploadSession createUploadSession(StorageUploadRequest request,
                                             Long userId) throws IOException;

    /**
     * Streams content into a pending proxy upload session.
     */
    FileStorage uploadFileContent(String uploadId,
                                  InputStream inputStream,
                                  Long userId) throws IOException;

    /**
     * Finalizes a pending direct upload session.
     */
    FileStorage completeUpload(String uploadId,
                               Long userId) throws IOException;
}
