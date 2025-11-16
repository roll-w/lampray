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

package tech.rollw.common.value;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a file size with human-readable formatting.
 *
 * @author RollW
 */
public final class FileSize implements Comparable<FileSize> {
    private final long bytes;

    public FileSize(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        this.bytes = bytes;
    }

    public long getBytes() {
        return bytes;
    }

    /**
     * Format the file size in human-readable format.
     */
    public String format() {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format("%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    @NotNull
    public String toString() {
        return format();
    }

    @Override
    public int compareTo(@NotNull FileSize other) {
        return Long.compare(bytes, other.bytes);
    }

    public static FileSize ofBytes(long bytes) {
        return new FileSize(bytes);
    }

    public static FileSize of(double size, Unit unit) {
        return new FileSize(unit.toBytes(size));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FileSize) obj;
        return this.bytes == that.bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytes);
    }


    public enum Unit {
        B(1),
        KB(1024),
        MB(1024 * 1024),
        GB(1024 * 1024 * 1024),
        TB(1024L * 1024L * 1024L * 1024L);

        private final long bytesFactor;

        Unit(long bytesFactor) {
            this.bytesFactor = bytesFactor;
        }

        public long toBytes(double size) {
            return (long) (size * bytesFactor);
        }

        public double toUnit(Unit targetUnit, double size) {
            return (double) (bytesFactor / targetUnit.bytesFactor) * size;
        }

        public static Unit fromString(String unit) {
            return switch (unit.toUpperCase()) {
                case "B" -> B;
                case "KB", "K" -> KB;
                case "MB", "M" -> MB;
                case "GB", "G" -> GB;
                case "TB", "T" -> TB;
                default -> throw new IllegalArgumentException("Unknown file size unit: " + unit);
            };
        }
    }
}

