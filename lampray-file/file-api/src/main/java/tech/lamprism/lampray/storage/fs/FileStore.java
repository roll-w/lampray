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

import space.lingu.NonNull;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface representing a file store that can store, retrieve, and manage files.
 *
 * @author RollW
 */
public interface FileStore {

    /**
     * Stores a file with the given name and input stream.
     *
     * @param path        the name of the file to store or the path depending on the file store
     *                    implementation
     * @param inputStream the input stream containing the file data
     * @return a {@link StoredFile} object representing the stored file
     * @throws FileStoreException if an error occurs while storing the file
     */
    @NonNull
    StoredFile storeFile(String path, InputStream inputStream) throws FileStoreException;

    void writeFileToStream(String path, OutputStream outputStream) throws FileStoreException;

    void writeFileToStream(String path, OutputStream outputStream,
                           PositionMark positionMark) throws FileStoreException;

    @NonNull
    StoredFile findStoredFile(String path) throws FileStoreException;

    @NonNull
    ID getId();

    boolean deleteFile(String path) throws FileStoreException;

    boolean exists(String path) throws FileStoreException;

    /**
     * Gets the total space available in this file store; -1 indicates that the space is not limited.
     *
     * @return the total space available in bytes, or -1 if the space is not limited.
     */
    long getAvailableSpace();

    boolean readOnly();

    /**
     * Represents a unique identifier for a file store.
     */
    class ID {
        @NonNull
        private final String type;

        @NonNull
        private final String name;

        public ID(@NonNull String type, @NonNull String name) {
            this.type = type;
            this.name = name;
        }

        @NonNull
        public String getType() {
            return type;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return type + ":" + name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ID id)) return false;
            return type.equals(id.type) && name.equals(id.name);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + name.hashCode();
        }

        public static ID of(String type, String name) {
            return new ID(type, name);
        }
    }
}
