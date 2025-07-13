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

import tech.lamprism.lampray.storage.fs.FileStore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class FileStoreGroup {
    private final String groupName;

    private record FileStoreEntry(FileStore fileStore, int priority) {
    }

    private final Map<FileStore.ID, FileStoreEntry> fileStoreMap;

    public FileStoreGroup(String groupName) {
        this.groupName = groupName;
        this.fileStoreMap = new ConcurrentHashMap<>();
    }

    public FileStoreGroup(String groupName, List<FileStore> fileStores, List<Integer> priorities) {
        this.groupName = groupName;
        this.fileStoreMap = new ConcurrentHashMap<>();
        if (fileStores.size() != priorities.size()) {
            throw new IllegalArgumentException("fileStores and priorities size mismatch");
        }
        for (int i = 0; i < fileStores.size(); i++) {
            FileStore store = fileStores.get(i);
            int priority = priorities.get(i);
            fileStoreMap.put(store.getId(), new FileStoreEntry(store, priority));
        }
    }

    /**
     * Finds the best FileStore for the given file size.
     * Priority is considered first; if no one is sufficient, returns the one with the most available space.
     *
     * @param fileSize the required file size in bytes
     * @return the best available FileStore
     * @throws IllegalStateException if no suitable FileStore is found
     */
    public FileStore findBest(long fileSize) {
        return fileStoreMap.values().stream()
                .filter(entry -> !entry.fileStore.readOnly())
                .sorted(Comparator.comparingInt(a -> a.priority))
                .filter(entry -> entry.fileStore.getAvailableSpace() == -1 ||
                        entry.fileStore.getAvailableSpace() >= fileSize)
                .map(entry -> entry.fileStore)
                .findFirst()
                .orElseGet(() -> fileStoreMap.values().stream()
                        .filter(entry -> !entry.fileStore.readOnly())
                        .max(Comparator.comparingLong(a -> a.fileStore.getAvailableSpace()))
                        .map(entry -> entry.fileStore)
                        .orElseThrow(() -> new IllegalStateException("No suitable FileStore found for size: " + fileSize))
                );
    }

    /**
     * Gets the group name of this FileStoreGroup.
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns a list of all FileStores in this group.
     *
     * @return list of FileStore
     */
    public List<FileStore> getFileStores() {
        return fileStoreMap.values().stream().map(entry -> entry.fileStore).toList();
    }

    public void addFileStore(FileStore fileStore) {
        addFileStore(fileStore, Integer.MAX_VALUE);
    }

    public void addFileStore(FileStore fileStore, int priority) {
        FileStore.ID id = fileStore.getId();
        if (fileStoreMap.containsKey(id)) {
            throw new IllegalArgumentException("FileStore with ID " + id + " already exists in group " + groupName);
        }
        fileStoreMap.put(id, new FileStoreEntry(fileStore, priority));
    }

    public boolean removeFileStore(FileStore.ID id) {
        return fileStoreMap.remove(id) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileStoreGroup that)) return false;
        return Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName);
    }
}
