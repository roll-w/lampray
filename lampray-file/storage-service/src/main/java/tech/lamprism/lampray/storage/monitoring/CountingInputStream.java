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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

public class CountingInputStream extends FilterInputStream {
    private final LongConsumer onBytesRead;

    public CountingInputStream(InputStream inputStream,
                               LongConsumer onBytesRead) {
        super(inputStream);
        this.onBytesRead = onBytesRead;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            onBytesRead.accept(1L);
        }
        return read;
    }

    @Override
    public int read(byte[] bytes,
                    int offset,
                    int length) throws IOException {
        int read = super.read(bytes, offset, length);
        if (read > 0) {
            onBytesRead.accept(read);
        }
        return read;
    }
}
