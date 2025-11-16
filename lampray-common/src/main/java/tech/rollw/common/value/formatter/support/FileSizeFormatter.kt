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

package tech.rollw.common.value.formatter.support

import tech.rollw.common.value.FileSize
import tech.rollw.common.value.formatter.ValueFormatter

/**
 * Formatter for [FileSize] values.
 * Formats file sizes in human-readable format (e.g., "1.5 GB").
 *
 * @author RollW
 */
class FileSizeFormatter : ValueFormatter<FileSize, String> {
    override fun format(value: FileSize): String {
        return value.format()
    }
}

