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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * @author RollW
 */
public class LocalStoredFile implements StoredFile {
    private final FileStore.ID id;
    private final File file;

    public LocalStoredFile(FileStore.ID id, File file) {
        this.id = id;
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public OffsetDateTime getLastModified() {
        long lastModified = file.lastModified();
        if (lastModified == 0) {
            return TimeAttributed.NONE_TIME;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastModified),
                ZoneId.systemDefault());
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        try {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime fileTime = basicFileAttributes.creationTime();
            if (fileTime == null) {
                return TimeAttributed.NONE_TIME;
            }
            return OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            return TimeAttributed.NONE_TIME;
        }
    }

    @Override
    public FileStore.ID getFileStoreId() {
        return id;
    }
}
