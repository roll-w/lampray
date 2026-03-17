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

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
            val output = ByteArrayOutputStream()

            source.writeTo(output, 2, 5)

            assertContentEquals("2345".toByteArray(), output.toByteArray())
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun `range aware factory uses custom opener`() {
        val source = InputStreamDownloadSource.rangeAware(
            inputStreamOpener = { "fallback".byteInputStream() },
            rangeInputStreamOpener = { startBytes, endBytes ->
                "$startBytes-$endBytes".byteInputStream()
            },
        )
        val output = ByteArrayOutputStream()

        source.writeTo(output, 3, 7)

        assertEquals("3-7", output.toString())
    }
}
