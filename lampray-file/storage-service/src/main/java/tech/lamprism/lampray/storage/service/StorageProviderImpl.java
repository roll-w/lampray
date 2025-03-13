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

package tech.lamprism.lampray.storage.service;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.TimeAttributed;
import tech.lamprism.lampray.storage.DefaultStorageIds;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileSummary;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageUrlProvider;
import tech.lamprism.lampray.storage.StorageUrlProviderStrategy;
import tech.rollw.common.web.DataErrorCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author RollW
 */
@Service
public class StorageProviderImpl implements StorageProvider, StorageUrlProvider {
    private StorageUrlProviderStrategy storageUrlProviderStrategy;

    public StorageProviderImpl(
            StorageUrlProviderStrategy storageUrlProviderStrategy) {
        this.storageUrlProviderStrategy = storageUrlProviderStrategy;
    }

    // TODO:
    @Override
    public FileSummary saveFile(InputStream inputStream)
            throws IOException {
        return null;
    }

    @Override
    public void getFile(String fileId, OutputStream outputStream) throws IOException {
        switch (fileId) {
            case DefaultStorageIds.DEFAULT_AVATAR_ID -> loadLocalResource(USER_AVATAR_PATH, outputStream);
            case DefaultStorageIds.DEFAULT_USER_COVER_ID,
                 DefaultStorageIds.DEFAULT_ARTICLE_COVER_ID -> loadLocalResource(USER_COVER_PATH, outputStream);
        }
    }

    @Override
    public void getFile(String fileId, OutputStream outputStream,
                        long startBytes, long endBytes) throws IOException {
        switch (fileId) {
            case DefaultStorageIds.DEFAULT_AVATAR_ID -> loadLocalResource(USER_AVATAR_PATH, outputStream,
                    startBytes, endBytes);
            case DefaultStorageIds.DEFAULT_USER_COVER_ID,
                 DefaultStorageIds.DEFAULT_ARTICLE_COVER_ID -> loadLocalResource(USER_COVER_PATH, outputStream,
                    startBytes, endBytes);
        }
    }

    @Override
    public long getFileSize(String fileId) {
        return 0;
    }

    private static final FileStorage AVATAR_FILE_STORAGE =
            FileStorage.builder()
                    .setFileId(DefaultStorageIds.DEFAULT_AVATAR_ID)
                    .setFileSize(0)
                    .setCreateTime(TimeAttributed.NONE_TIME)
                    .setMimeType("image/png")
                    .setFileType(FileType.IMAGE)
                    .build();

    private static final FileStorage USER_COVER_FILE_STORAGE =
            FileStorage.builder()
                    .setFileId(DefaultStorageIds.DEFAULT_USER_COVER_ID)
                    .setFileSize(0)
                    .setCreateTime(TimeAttributed.NONE_TIME)
                    .setMimeType("image/png")
                    .setFileType(FileType.IMAGE)
                    .build();

    @Override
    public FileStorage getFileStorage(String fileId) {
        switch (fileId) {
            case DefaultStorageIds.DEFAULT_AVATAR_ID -> {
                return AVATAR_FILE_STORAGE;
            }
            case DefaultStorageIds.DEFAULT_USER_COVER_ID -> {
                return USER_COVER_FILE_STORAGE;
            }
        }

        throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                "Not found file storage: " + fileId);
    }

    private static final String USER_AVATAR_PATH = "/static/images/user-avatar.png";
    private static final String USER_COVER_PATH = "/static/images/user-cover.png";

    private static final int BUFFER_SIZE = 8192;

    private void loadLocalResource(String path, OutputStream stream)
            throws IOException {
        try (InputStream inputStream =
                     getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Not found resource: " + path);
            }
            inputStream.transferTo(stream);
        }
    }

    private void loadLocalResource(String path, OutputStream stream,
                                   long startBytes, long endBytes) throws IOException {
        try (InputStream inputStream =
                     getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Not found resource: " + path);
            }
            long skipped = inputStream.skip(startBytes);
            if (skipped != startBytes) {
                throw new IOException("Skipped error");
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long left = endBytes - startBytes + 1;
            while ((read = inputStream.read(buffer)) != -1) {
                if (read > left) {
                    read = (int) left;
                }
                stream.write(buffer, 0, read);
                left -= read;
                if (left == 0) {
                    break;
                }
            }
        }
    }

    @Override
    public String getUrlOfStorage(String id) {
        return storageUrlProviderStrategy.getUrlOfStorage(id);
    }

    @Override
    public void setStorageUrlProviderStrategy(StorageUrlProviderStrategy storageUrlProviderStrategy) {
        this.storageUrlProviderStrategy = storageUrlProviderStrategy;
    }
}
