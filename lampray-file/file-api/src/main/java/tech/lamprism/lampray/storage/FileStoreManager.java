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
import tech.lamprism.lampray.storage.fs.FileStore;
import tech.lamprism.lampray.storage.fs.FileStoreException;
import tech.lamprism.lampray.storage.fs.PositionMark;
import tech.lamprism.lampray.storage.fs.StoredFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author RollW
 */
public interface FileStoreManager {
    FileStore getFileStore(FileStore.ID id);

    /**
     * Get a file store group by group name.
     *
     * @param groupName the group name
     * @return the file store group, or null if not found
     */
    FileStoreGroup getGroup(String groupName);

    /**
     * Create a new file store group. Throws if a group already exists.
     *
     * @param name the name of the group
     * @return the created file store group
     */
    FileStoreGroup createGroup(String name);

    /**
     * Store file to all groups (multi-backup), each group chooses the file store with the most available space.
     */
    List<StoredFile> storeFile(String fileName, InputStream inputStream) throws IOException;

    void writeFileToStream(String path, OutputStream outputStream) throws FileStoreException;

    void writeFileToStream(String path, OutputStream outputStream,
                           PositionMark positionMark) throws FileStoreException;

    @NonNull
    StoredFile findStoredFile(String path) throws FileStoreException;
}
