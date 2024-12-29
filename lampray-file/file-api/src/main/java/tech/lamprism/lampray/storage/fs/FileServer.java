/*
 * Copyright (C) 2023 RollW
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a file server that can upload and download files.
 *
 * @author RollW
 */
public interface FileServer {
    @NonNull
    default ServerFile upload(@NonNull String qualifiedName, @NonNull InputStream inputStream)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    void writeFileTo(@NonNull String qualifiedName,
                     @NonNull OutputStream outputStream,
                     long start, long length) throws IOException;

    void writeFileTo(@NonNull String qualifiedName,
                     @NonNull OutputStream outputStream) throws IOException;

    String getName();

    boolean supports(ServerFile file);
}
