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

package tech.lamprism.lampray.storage.fs;

import tech.lamprism.lampray.TimeAttributed;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class ClassPathStoredFile implements StoredFile {
    private final FileStore.ID fileStoreId;
    private final String path;
    private final long size;

    public ClassPathStoredFile(FileStore.ID fileStoreId, String path,
                               long size) {
        this.fileStoreId = fileStoreId;
        this.path = path;
        this.size = size;
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public OffsetDateTime getLastModified() {
        return TimeAttributed.NONE_TIME;
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return TimeAttributed.NONE_TIME;
    }

    @Override
    public FileStore.ID getFileStoreId() {
        return fileStoreId;
    }
}
