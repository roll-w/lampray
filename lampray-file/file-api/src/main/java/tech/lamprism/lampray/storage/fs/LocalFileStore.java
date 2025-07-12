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

import tech.lamprism.lampray.storage.PositionMark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author RollW
 */
public class LocalFileStore implements FileStore {
    private final File root;
    private final ID id;

    public LocalFileStore(File root) {
        this.root = root;
        this.id = ID.of("LocalFileStore", root.getAbsolutePath());
        init();
    }

    public LocalFileStore(String root) {
        this(new File(root));
    }

    public File getRoot() {
        return root;
    }

    private void init() {
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new IllegalStateException("Failed to create directory: " + root.getAbsolutePath());
            }
        } else if (!root.isDirectory()) {
            throw new IllegalStateException("The specified path is not a directory: " + root.getAbsolutePath());
        }
    }

    @Override
    public StoredFile storeFile(String path, InputStream inputStream) throws IOException {
        File file = new File(root, path);
        if (file.exists() && !file.isFile()) {
            throw new FileStoreException("A file with the same name exists but is not a regular file: " + file.getAbsolutePath());
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new FileStoreException("Failed to create file: " + file.getAbsolutePath());
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            inputStream.transferTo(outputStream);
        }

        return new LocalStoredFile(getId(), file);
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream) throws IOException {
        File file = new File(root, path);
        if (!file.exists() || !file.isFile()) {
            throw new FileStoreException("File not found or is not a regular file: " + file.getAbsolutePath());
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            inputStream.transferTo(outputStream);
        }
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream, PositionMark positionMark) throws IOException {
        File file = new File(root, path);
        if (!file.exists() || !file.isFile()) {
            throw new FileStoreException("File not found or is not a regular file: " + file.getAbsolutePath());
        }
        long length = file.length();
        positionMark.checkValid(length);
        if (positionMark.getLength() == 0) {
            return; // No data to write
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            long skip = inputStream.skip(positionMark.getOffset());
            if (skip < positionMark.getOffset()) {
                throw new FileStoreException("Failed to skip to the specified offset: " + positionMark.getOffset());
            }
            long bytesToRead = positionMark.getLength();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesToRead -= bytesRead;
            }
        }
    }

    @Override
    public StoredFile findStoredFile(String path) {
        File file = new File(root, path);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return new LocalStoredFile(getId(), file);
    }

    @Override
    public ID getId() {
        return id;
    }

    @Override
    public boolean deleteFile(String path) throws FileStoreException {
        File file = new File(root, path);
        if (!file.exists()) {
            return false;
        }
        if (!file.isFile()) {
            throw new FileStoreException("The specified path is not a regular file: " + file.getAbsolutePath());
        }
        return file.delete();
    }

    @Override
    public boolean exists(String path) {
        File file = new File(root, path);
        return file.exists() && file.isFile();
    }

    @Override
    public long getAvailableSpace() {
        return root.getUsableSpace();
    }

    @Override
    public boolean readOnly() {
        return false;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
