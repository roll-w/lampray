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

package tech.lamprism.lampray.storage.source;

import tech.lamprism.lampray.storage.StorageDownloadSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * @author RollW
 */
public final class InputStreamDownloadSource implements StorageDownloadSource {
    private static final int BUFFER_SIZE = 8192;

    private final InputStreamOpener inputStreamOpener;
    private final RangeInputStreamOpener rangeInputStreamOpener;

    public static InputStreamDownloadSource from(InputStreamOpener inputStreamOpener) {
        return new InputStreamDownloadSource(inputStreamOpener);
    }

    public static InputStreamDownloadSource rangeAware(InputStreamOpener inputStreamOpener,
                                                       RangeInputStreamOpener rangeInputStreamOpener) {
        return new InputStreamDownloadSource(inputStreamOpener, rangeInputStreamOpener);
    }

    public static InputStreamDownloadSource fromPath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new InputStreamDownloadSource(
                () -> Files.newInputStream(path),
                (startBytes, endBytes) -> openRange(path, startBytes, endBytes)
        );
    }

    public InputStreamDownloadSource(InputStreamOpener inputStreamOpener) {
        this(inputStreamOpener, null);
    }

    public InputStreamDownloadSource(InputStreamOpener inputStreamOpener,
                                     RangeInputStreamOpener rangeInputStreamOpener) {
        this.inputStreamOpener = Objects.requireNonNull(inputStreamOpener, "inputStreamOpener must not be null");
        this.rangeInputStreamOpener = rangeInputStreamOpener;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        try (InputStream inputStream = inputStreamOpener.open()) {
            inputStream.transferTo(outputStream);
        }
    }

    @Override
    public void writeTo(OutputStream outputStream,
                        long startBytes,
                        long endBytes) throws IOException {
        if (startBytes < 0) {
            throw new IllegalArgumentException("startBytes must not be negative");
        }
        if (endBytes < startBytes) {
            throw new IllegalArgumentException("endBytes must be greater than or equal to startBytes");
        }

        if (rangeInputStreamOpener != null) {
            try (InputStream inputStream = rangeInputStreamOpener.open(startBytes, endBytes)) {
                inputStream.transferTo(outputStream);
            }
            return;
        }

        try (InputStream inputStream = inputStreamOpener.open()) {
            skipExactly(inputStream, startBytes);
            copyRange(inputStream, outputStream, endBytes - startBytes + 1);
        }
    }

    private void skipExactly(InputStream inputStream,
                             long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            if (inputStream.read() == -1) {
                throw new IOException("Failed to seek stream to requested range.");
            }
            remaining--;
        }
    }

    private void copyRange(InputStream inputStream,
                           OutputStream outputStream,
                           long bytes) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long remaining = bytes;
        while (remaining > 0) {
            int read = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read == -1) {
                break;
            }
            outputStream.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private static InputStream openRange(Path path,
                                         long startBytes,
                                         long endBytes) throws IOException {
        SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
        channel.position(startBytes);
        return new RangeChannelInputStream(channel, endBytes - startBytes + 1);
    }

    @FunctionalInterface
    public interface InputStreamOpener {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    public interface RangeInputStreamOpener {
        InputStream open(long startBytes,
                         long endBytes) throws IOException;
    }

    private static final class RangeChannelInputStream extends InputStream {
        private final SeekableByteChannel channel;
        private final InputStream delegate;
        private long remaining;

        private RangeChannelInputStream(SeekableByteChannel channel,
                                        long remaining) {
            this.channel = channel;
            this.delegate = Channels.newInputStream(channel);
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer,
                        int offset,
                        int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}
