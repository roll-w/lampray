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

package tech.lamprism.lampray.storage;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.EntityBuilder;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
public class FileStorage implements DataEntity<String> {
    private final String fileId;
    private final long fileSize;
    private final String mimeType;
    private final FileType fileType;
    private final OffsetDateTime createTime;

    public FileStorage(String fileId,
                       long fileSize,
                       String mimeType,
                       FileType fileType,
                       OffsetDateTime createTime) {
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.createTime = createTime;
    }

    public String getFileId() {
        return fileId;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String getEntityId() {
        return fileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileType getFileType() {
        return fileType;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return createTime;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileStorage that)) return false;
        return fileSize == that.fileSize && createTime == that.createTime && Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, fileSize, createTime);
    }

    @Override
    public String toString() {
        return "FileStorage{" +
                "fileId='" + fileId + '\'' +
                ", fileSize=" + fileSize +
                ", createTime=" + createTime +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return StorageResourceKind.INSTANCE;
    }

    public static final class Builder implements EntityBuilder<FileStorage, String> {
        private String fileId;
        private long fileSize;
        private String mimeType;
        private FileType fileType;
        private OffsetDateTime createTime;

        public Builder() {
        }

        public Builder(FileStorage fileStorage) {
            this.fileId = fileStorage.fileId;
            this.fileSize = fileStorage.fileSize;
            this.mimeType = fileStorage.mimeType;
            this.fileType = fileStorage.fileType;
            this.createTime = fileStorage.createTime;
        }

        @Override
        public Builder setEntityId(String string) {
            return setFileId(string);
        }

        public Builder setFileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder setFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setFileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder setCreateTime(OffsetDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public FileStorage build() {
            return new FileStorage(fileId, fileSize, mimeType, fileType, createTime);
        }
    }
}
