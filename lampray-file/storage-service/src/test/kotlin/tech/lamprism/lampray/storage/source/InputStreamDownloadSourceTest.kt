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

package tech.lamprism.lampray.storage.source

import tech.lamprism.lampray.storage.StorageByteRange
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * @author RollW
 */
class InputStreamDownloadSourceTest {
    @Test
    fun `from path supports range writes`() {
        val path = Files.createTempFile("lampray-source-", ".bin")
        try {
            Files.writeString(path, "0123456789")
            val source = InputStreamDownloadSource.fromPath(path)

            source.openStream(StorageByteRange(2, 5)).use { inputStream ->
                assertContentEquals("2345".toByteArray(), inputStream.readBytes())
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `range aware factory uses custom opener`() {
        val source = InputStreamDownloadSource.rangeAware(
            { "fallback".byteInputStream() },
            { range ->
                "${range.startBytes}-${range.endBytes}".byteInputStream()
            },
        )

        val output = ByteArrayOutputStream()
        source.openStream(StorageByteRange(3, 7)).use { inputStream ->
            inputStream.transferTo(output)
        }

        assertEquals("3-7", output.toString())
    }

    @Test
    fun `range read fails when requested bytes exceed source size`() {
        val source = InputStreamDownloadSource.from {
            "abc".byteInputStream()
        }

        assertFailsWith<IOException> {
            source.openStream(StorageByteRange(1, 5)).use { inputStream ->
                inputStream.readBytes()
            }
        }
    }
}
