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
    @NonNull
    public StoredFile storeFile(String path, InputStream inputStream) throws FileStoreException {
        File file = new File(root, path);
        if (file.exists() && !file.isFile()) {
            throw new FileStoreException("A file with the same name exists but is not a regular file: " + file.getAbsolutePath());
        }
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new FileStoreException("Failed to create file: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new FileStoreException("Failed to create file: " + file.getAbsolutePath(), e);
        }

        try (OutputStream outputStream = new FileOutputStream(file)) {
            inputStream.transferTo(outputStream);
        } catch (IOException e) {
            throw new FileStoreException("Failed to write file: " + file.getAbsolutePath(), e);
        }

        return new LocalStoredFile(getId(), file);
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream) throws FileStoreException {
        File file = new File(root, path);
        if (!file.exists() || !file.isFile()) {
            throw new FileStoreException("File not found or is not a regular file: " + file.getAbsolutePath());
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            IOUtils.transferTo(inputStream, outputStream);
        } catch (IOException e) {
            throw new FileStoreException("Failed to write file to stream: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream, PositionMark positionMark) throws FileStoreException {
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
            IOUtils.transferTo(inputStream, outputStream, positionMark);
        } catch (IOException e) {
            throw new FileStoreException("Failed to write file to stream: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    @NonNull
    public StoredFile findStoredFile(String path) throws FileStoreException {
        File file = new File(root, path);
        if (!file.exists() || !file.isFile()) {
            throw new FileStoreException("File not found or is not a regular file: " + file.getAbsolutePath());
        }
        return new LocalStoredFile(getId(), file);
    }

    @Override
    @NonNull
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
