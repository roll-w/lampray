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

package tech.lamprism.lampray.storage.materialization;

import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobObject;

import java.nio.file.Path;

import java.util.Objects;

/**
 * Describes the intent for materializing a blob into one or more backends.
 *
 * @author RollW
 */
public final class BlobMaterializationRequest {
    private final StorageWritePlan writePlan;
    private final String mimeType;
    private final FileType fileType;
    private final long size;
    private final String checksum;
    private final BlobMaterializationSource source;

    private BlobMaterializationRequest(StorageWritePlan writePlan,
                                       String mimeType,
                                       FileType fileType,
                                       long size,
                                       String checksum,
                                       BlobMaterializationSource source) {
        this.writePlan = Objects.requireNonNull(writePlan, "writePlan must not be null");
        this.mimeType = Objects.requireNonNull(mimeType, "mimeType must not be null");
        this.fileType = Objects.requireNonNull(fileType, "fileType must not be null");
        this.size = size;
        this.checksum = Objects.requireNonNull(checksum, "checksum must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    public static BlobMaterializationRequest forTempUpload(StorageWritePlan writePlan,
                                                           String mimeType,
                                                           FileType fileType,
                                                           long size,
                                                           String checksum,
                                                           Path tempPath) {
        return new BlobMaterializationRequest(
                writePlan,
                mimeType,
                fileType,
                size,
                checksum,
                new TempFileBlobSource(tempPath)
        );
    }

    public static BlobMaterializationRequest forUploadedObject(StorageWritePlan writePlan,
                                                               String mimeType,
                                                               FileType fileType,
                                                               long size,
                                                               String checksum,
                                                               BlobObject uploadedObject) {
        return new BlobMaterializationRequest(
                writePlan,
                mimeType,
                fileType,
                size,
                checksum,
                new UploadedBlobSource(uploadedObject)
        );
    }

    public StorageWritePlan writePlan() {
        return writePlan;
    }

    public String mimeType() {
        return mimeType;
    }

    public FileType fileType() {
        return fileType;
    }

    public long size() {
        return size;
    }

    public String checksum() {
        return checksum;
    }

    public BlobMaterializationSource source() {
        return source;
    }

    public String groupName() {
        return writePlan.getGroupConfig().getName();
    }

    public String primaryBackend() {
        return writePlan.getPrimaryBackend();
    }
}
