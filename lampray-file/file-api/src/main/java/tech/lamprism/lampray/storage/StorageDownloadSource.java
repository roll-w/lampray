/*
 * Copyright (C) 2023-2026 RollW
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Streaming source for proxy downloads.
 *
 * <p>Callers may either open the stream directly or let the source transfer its data to an
 * {@link OutputStream}. Range-aware downloads use {@link StorageByteRange} so higher layers do not
 * leak transport-specific details into storage backends.</p>
 *
 * @author RollW
 */
public interface StorageDownloadSource {
    InputStream openStream() throws IOException;

    InputStream openStream(StorageByteRange range) throws IOException;

    default void transferTo(OutputStream outputStream) throws IOException {
        try (InputStream inputStream = openStream()) {
            inputStream.transferTo(Objects.requireNonNull(outputStream, "outputStream must not be null"));
        }
    }

    default void transferTo(OutputStream outputStream,
                            StorageByteRange range) throws IOException {
        try (InputStream inputStream = openStream(Objects.requireNonNull(range, "range must not be null"))) {
            inputStream.transferTo(Objects.requireNonNull(outputStream, "outputStream must not be null"));
        }
    }
}
