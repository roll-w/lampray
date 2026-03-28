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

package tech.lamprism.lampray.storage.monitoring;

import tech.lamprism.lampray.storage.StorageByteRange;
import tech.lamprism.lampray.storage.StorageDownloadSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

public class CountingStorageDownloadSource implements StorageDownloadSource {
    private final StorageDownloadSource delegate;
    private final Runnable onOpen;
    private final LongConsumer onBytesRead;

    public CountingStorageDownloadSource(StorageDownloadSource delegate,
                                         Runnable onOpen,
                                         LongConsumer onBytesRead) {
        this.delegate = delegate;
        this.onOpen = onOpen;
        this.onBytesRead = onBytesRead;
    }

    @Override
    public InputStream openStream() throws IOException {
        onOpen.run();
        return new CountingInputStream(delegate.openStream(), onBytesRead);
    }

    @Override
    public InputStream openStream(StorageByteRange range) throws IOException {
        onOpen.run();
        return new CountingInputStream(delegate.openStream(range), onBytesRead);
    }
}
