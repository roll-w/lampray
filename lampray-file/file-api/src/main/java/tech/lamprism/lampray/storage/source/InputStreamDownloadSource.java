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

import space.lingu.NonNull;
import tech.lamprism.lampray.storage.StorageByteRange;
import tech.lamprism.lampray.storage.StorageDownloadSource;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * {@link StorageDownloadSource} implementation backed by {@link InputStream} openers.
 *
 * <p>The class composes different range strategies so callers can always open a full stream or a
 * byte-range stream, while storage backends stay free to provide an optimized implementation.</p>
 *
 * @author RollW
 */
public final class InputStreamDownloadSource implements StorageDownloadSource {
    private final RangeStreamAccess rangeStreamAccess;

    public static InputStreamDownloadSource from(InputStreamOpener inputStreamOpener) {
        return new InputStreamDownloadSource(inputStreamOpener);
    }

    public static InputStreamDownloadSource rangeAware(InputStreamOpener inputStreamOpener,
                                                       RangeInputStreamOpener rangeInputStreamOpener) {
        return new InputStreamDownloadSource(inputStreamOpener, rangeInputStreamOpener);
    }

    public static InputStreamDownloadSource fromPath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new InputStreamDownloadSource(new SeekablePathStreamAccess(path));
    }

    public InputStreamDownloadSource(InputStreamOpener inputStreamOpener) {
        this(new FallbackRangeStreamAccess(inputStreamOpener));
    }

    public InputStreamDownloadSource(InputStreamOpener inputStreamOpener,
                                     RangeInputStreamOpener rangeInputStreamOpener) {
        this(new DelegatingRangeStreamAccess(inputStreamOpener, rangeInputStreamOpener));
    }

    private InputStreamDownloadSource(RangeStreamAccess rangeStreamAccess) {
        this.rangeStreamAccess = Objects.requireNonNull(rangeStreamAccess, "rangeStreamAccess must not be null");
    }

    @Override
    public InputStream openStream() throws IOException {
        return rangeStreamAccess.openStream();
    }

    @Override
    public InputStream openStream(StorageByteRange range) throws IOException {
        return rangeStreamAccess.openStream(Objects.requireNonNull(range, "range must not be null"));
    }

    private interface RangeStreamAccess {
        InputStream openStream() throws IOException;

        InputStream openStream(StorageByteRange range) throws IOException;
    }

    private static final class FallbackRangeStreamAccess implements RangeStreamAccess {
        private final InputStreamOpener inputStreamOpener;

        private FallbackRangeStreamAccess(InputStreamOpener inputStreamOpener) {
            this.inputStreamOpener = Objects.requireNonNull(inputStreamOpener, "inputStreamOpener must not be null");
        }

        @Override
        public InputStream openStream() throws IOException {
            return inputStreamOpener.open();
        }

        @Override
        public InputStream openStream(StorageByteRange range) throws IOException {
            InputStream inputStream = inputStreamOpener.open();
            try {
                skipExactly(inputStream, range.startBytes());
                return new BoundedInputStream(inputStream, range.length());
            } catch (IOException | RuntimeException exception) {
                inputStream.close();
                throw exception;
            }
        }
    }

    private static final class DelegatingRangeStreamAccess implements RangeStreamAccess {
        private final InputStreamOpener inputStreamOpener;
        private final RangeInputStreamOpener rangeInputStreamOpener;

        private DelegatingRangeStreamAccess(InputStreamOpener inputStreamOpener,
                                            RangeInputStreamOpener rangeInputStreamOpener) {
            this.inputStreamOpener = Objects.requireNonNull(inputStreamOpener, "inputStreamOpener must not be null");
            this.rangeInputStreamOpener = Objects.requireNonNull(
                    rangeInputStreamOpener,
                    "rangeInputStreamOpener must not be null"
            );
        }

        @Override
        public InputStream openStream() throws IOException {
            return inputStreamOpener.open();
        }

        @Override
        public InputStream openStream(StorageByteRange range) throws IOException {
            return rangeInputStreamOpener.open(range);
        }
    }

    private static final class SeekablePathStreamAccess implements RangeStreamAccess {
        private final Path path;

        private SeekablePathStreamAccess(Path path) {
            this.path = Objects.requireNonNull(path, "path must not be null");
        }

        @Override
        public InputStream openStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public InputStream openStream(StorageByteRange range) throws IOException {
            SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
            try {
                channel.position(range.startBytes());
                return new BoundedInputStream(Channels.newInputStream(channel), range.length());
            } catch (IOException | RuntimeException exception) {
                channel.close();
                throw exception;
            }
        }
    }

    private static void skipExactly(InputStream inputStream,
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

    @FunctionalInterface
    public interface InputStreamOpener {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    public interface RangeInputStreamOpener {
        InputStream open(StorageByteRange range) throws IOException;
    }

    private static final class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        private BoundedInputStream(InputStream delegate,
                                   long remaining) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value == -1) {
                throw new EOFException("Requested range exceeds available bytes.");
            }
            remaining--;
            return value;
        }

        @Override
        public int read(@NonNull byte[] buffer,
                        int offset,
                        int length) throws IOException {
            Objects.requireNonNull(buffer, "buffer must not be null");
            if (length == 0) {
                return 0;
            }
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(buffer, offset, (int) Math.min(length, remaining));
            if (read == -1) {
                throw new EOFException("Requested range exceeds available bytes.");
            }
            remaining -= read;
            return read;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
