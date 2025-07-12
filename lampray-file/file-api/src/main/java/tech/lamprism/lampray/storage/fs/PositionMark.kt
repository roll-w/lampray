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

package tech.lamprism.lampray.storage.fs

/**
 * @author RollW
 */
data class PositionMark(
    val offset: Long,
    val length: Long
) {
    val start get() = offset
    val end get() = offset + length

    fun checkValid(fileSize: Long) {
        if (offset < 0 || length < 0) {
            throw IllegalArgumentException("PositionMark offset and length must be non-negative")
        }
        if (offset >= fileSize) {
            throw IllegalArgumentException("PositionMark offset is out of bounds: offset=$offset, fileSize=$fileSize")
        }
        if (offset + length > fileSize) {
            throw IllegalArgumentException("PositionMark exceeds file size: offset=$offset, length=$length, fileSize=$fileSize")
        }
    }

    companion object {
        fun fromOffset(offset: Long, length: Long): PositionMark {
            return PositionMark(offset, length)
        }

        fun fromStartEnd(start: Long, end: Long): PositionMark {
            return PositionMark(start, end - start)
        }
    }
}