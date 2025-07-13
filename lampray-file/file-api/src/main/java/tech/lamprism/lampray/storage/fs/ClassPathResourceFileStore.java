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
 * @author RollW
 */
public class ClassPathResourceFileStore extends ReadonlyFileStore {

    public ClassPathResourceFileStore() {
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream) throws FileStoreException {
        InputStream inputStream = getClass().getResourceAsStream(path);
        if (inputStream == null) {
            throw new FileStoreException("Resource not found: " + path);
        }
        try (inputStream) {
            IOUtils.transferTo(inputStream, outputStream);
        } catch (Exception e) {
            throw new FileStoreException("Error writing resource to stream: " + path, e);
        }
    }

    @Override
    public void writeFileToStream(String path, OutputStream outputStream, PositionMark positionMark) throws FileStoreException {
        InputStream inputStream = getClass().getResourceAsStream(path);
        if (inputStream == null) {
            throw new FileStoreException("Resource not found: " + path);
        }
        try (inputStream) {
            long length = inputStream.available();
            positionMark.checkValid(length);
            if (positionMark.getLength() == 0) {
                return; // No data to write
            }
            IOUtils.transferTo(inputStream, outputStream, positionMark);
        } catch (Exception e) {
            throw new FileStoreException("Error writing resource to stream: " + path, e);
        }

    }

    private long determineFileSize(String path) {
        InputStream inputStream = getClass().getResourceAsStream(path);
        if (inputStream == null) {
            return -1;
        }
        try (inputStream) {
            return inputStream.available();
        } catch (Exception e) {
            return -1; // Unable to determine size
        }
    }

    @Override
    @NonNull
    public StoredFile findStoredFile(String path) throws FileStoreException {
        if (exists(path)) {
            return new ClassPathStoredFile(getId(), path, determineFileSize(path));
        }
        throw new FileStoreException("Resource not found: " + path);
    }

    @Override
    @NonNull
    public ID getId() {
        return ID.of("ClassPath", "default");
    }

    @Override
    public boolean exists(String path) {
        return getClass().getResource(path) != null;
    }
}
