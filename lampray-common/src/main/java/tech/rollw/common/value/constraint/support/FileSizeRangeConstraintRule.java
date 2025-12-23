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

package tech.rollw.common.value.constraint.support;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.rollw.common.value.FileSize;
import tech.rollw.common.value.constraint.ValueConstraintRule;
import tech.rollw.common.value.constraint.ValueValidationResult;

/**
 * @author RollW
 */
public class FileSizeRangeConstraintRule implements ValueConstraintRule<FileSize> {
    private static final String TYPE = "fileSizeRange";

    private final FileSize min;
    private final FileSize max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    public FileSizeRangeConstraintRule(@Nullable FileSize min, @Nullable FileSize max) {
        this(min, max, true, true);
    }

    public FileSizeRangeConstraintRule(@Nullable FileSize min, @Nullable FileSize max,
                                        boolean minInclusive, boolean maxInclusive) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Min file size must not be greater than max file size");
        }
        this.min = min;
        this.max = max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public static FileSizeRangeConstraintRule atLeast(FileSize min) {
        return new FileSizeRangeConstraintRule(min, null, true, true);
    }

    public static FileSizeRangeConstraintRule atMost(FileSize max) {
        return new FileSizeRangeConstraintRule(null, max, true, true);
    }

    public static FileSizeRangeConstraintRule between(FileSize min, FileSize max) {
        return new FileSizeRangeConstraintRule(min, max, true, true);
    }

    @NonNull
    @Override
    public String getType() {
        return TYPE;
    }

    @NonNull
    @Override
    public ValueValidationResult validate(@Nullable FileSize value) {
        if (value == null) {
            return ValueValidationResult.failure("File size cannot be null");
        }

        if (min != null) {
            int compare = value.compareTo(min);
            if (minInclusive ? compare < 0 : compare <= 0) {
                return ValueValidationResult.failure(
                        "File size must be " + (minInclusive ? ">=" : ">") + " " + formatFileSize(min) +
                                ", but was " + formatFileSize(value)
                );
            }
        }

        if (max != null) {
            int compare = value.compareTo(max);
            if (maxInclusive ? compare > 0 : compare >= 0) {
                return ValueValidationResult.failure(
                        "File size must be " + (maxInclusive ? "<=" : "<") + " " + formatFileSize(max) +
                                ", but was " + formatFileSize(value)
                );
            }
        }

        return ValueValidationResult.success();
    }

    @NonNull
    @Override
    public Descriptor getDescriptor() {
        return new FileSizeRangeDescriptor(min, max, minInclusive, maxInclusive);
    }

    private String formatFileSize(FileSize fileSize) {
        return fileSize == null ? "null" : fileSize.toString();
    }

    public static class FileSizeRangeDescriptor implements Descriptor {
        private final FileSize min;
        private final FileSize max;
        private final boolean minInclusive;
        private final boolean maxInclusive;

        public FileSizeRangeDescriptor(FileSize min, FileSize max,
                                       boolean minInclusive, boolean maxInclusive) {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }

        public FileSize getMin() {
            return min;
        }

        public FileSize getMax() {
            return max;
        }

        public boolean isMinInclusive() {
            return minInclusive;
        }

        public boolean isMaxInclusive() {
            return maxInclusive;
        }

        @Override
        public String toString() {
            return "FileSizeRange{" +
                    (minInclusive ? "[" : "(") +
                    (min != null ? formatOrNull(min) : "-") + ", " +
                    (max != null ? formatOrNull(max) : "+") +
                    (maxInclusive ? "]" : ")") +
                    "}";
        }

        private String formatOrNull(FileSize fileSize) {
            return fileSize == null ? "null" : fileSize.toString();
        }
    }
}
