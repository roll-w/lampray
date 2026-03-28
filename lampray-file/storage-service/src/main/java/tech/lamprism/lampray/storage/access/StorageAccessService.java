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

package tech.lamprism.lampray.storage.access;

import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;

import java.io.IOException;

/**
 * Contract for resolving storage downloads and references.
 *
 * @author RollW
 */
public interface StorageAccessService {
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException;

    public StorageReference resolveStorageReference(String id,
                                                    StorageReferenceRequest request,
                                                    Long userId) throws IOException;
}
