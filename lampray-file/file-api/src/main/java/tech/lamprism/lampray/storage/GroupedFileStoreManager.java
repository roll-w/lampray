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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileStoreManager implementation using group-based logic.
 * Supports multi-backup and group-based registration/removal.
 *
 * @author RollW
 */
public class GroupedFileStoreManager implements FileStoreManager {
    private final Map<String, FileStoreGroup> groupMap = new ConcurrentHashMap<>();
    private final Map<FileStore.ID, FileStore> fileStoreMap = new ConcurrentHashMap<>();

    public GroupedFileStoreManager(List<FileStoreGroup> groups) {
        for (FileStoreGroup group : groups) {
            groupMap.put(group.getGroupName(), group);
            for (FileStore store : group.getFileStores()) {
                fileStoreMap.put(store.getId(), store);
            }
        }
    }

    @Override
    public FileStore getFileStore(FileStore.ID id) {
        FileStore store = fileStoreMap.get(id);
        if (store == null) {
            throw new IllegalArgumentException("File store not found: " + id);
        }
        return store;
    }

    public FileStoreGroup getGroup(String groupName) {
        return groupMap.get(groupName);
    }

    @Override
    public FileStoreGroup createGroup(String name) {
        if (groupMap.containsKey(name)) {
            throw new IllegalArgumentException("Group already exists: " + name);
        }
        FileStoreGroup group = new FileStoreGroup(name);
        groupMap.put(name, group);
        return group;
    }

    /**
     * Store file to all groups (multi-backup), each group chooses the file store with the most available space.
     */
    @Override
    public List<StoredFile> storeFile(String fileName, InputStream inputStream) throws IOException {
        List<StoredFile> results = new ArrayList<>();

        long length = inputStream.available();
        for (FileStoreGroup group : groupMap.values()) {
            FileStore best = group.findBest(length);
            StoredFile stored = best.storeFile(fileName, inputStream);
            inputStream.reset();
            results.add(stored);
        }
        return results;
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream) throws FileStoreException {
        FileStore fileStore = findFileStore(path);
        fileStore.writeFileToStream(path, outputStream);
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream, PositionMark positionMark) throws FileStoreException {
        FileStore fileStore = findFileStore(path);
        fileStore.writeFileToStream(path, outputStream, positionMark);
    }

    @NonNull
    @Override
    public StoredFile findStoredFile(String path) throws FileStoreException {
        FileStore fileStore = findFileStore(path);
        return fileStore.findStoredFile(path);
    }

    private FileStore findFileStore(String path) throws FileStoreException {
        for (FileStoreGroup group : groupMap.values()) {
            for (FileStore fs : group.getFileStores()) {
                if (fs.exists(path)) {
                    return fs;
                }
            }
        }
        throw new FileStoreException("File not found: " + path);
    }
}
