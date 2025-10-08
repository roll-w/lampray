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

package tech.lamprism.lampray.content.structuraltext.codec

import com.fasterxml.jackson.databind.ObjectMapper
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextParser
import tech.lamprism.lampray.content.structuraltext.StructuralTextRenderer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/**
 * Compressed (GZIP + Base64) JSON serializer for [tech.lamprism.lampray.content.structuraltext.StructuralText].
 *
 * @author RollW
 */
class StructuralTextCompressedSerializer(
    private val objectMapper: ObjectMapper
) : StructuralTextRenderer, StructuralTextParser {
    override fun render(text: StructuralText): String {
        val bytes = objectMapper.writeValueAsBytes(text)
        val compressed = ByteArrayOutputStream().use { byteArrayOutputStream ->
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(bytes)
            }
            byteArrayOutputStream.toByteArray()
        }
        return encodeToBase85(compressed)
    }

    override fun parse(input: String): StructuralText {
        val compressed = decodeBase85(input)
        val bytes = compressed.inputStream().use { inputStream ->
            GZIPInputStream(inputStream).use { gzipInputStream ->
                gzipInputStream.readBytes()
            }
        }
        return objectMapper.readValue(bytes, StructuralText::class.java)
    }

    private fun decodeBase85(ascii85: String): ByteArray {
        val byteIn = ByteArrayInputStream(ascii85.toByteArray(StandardCharsets.US_ASCII))
        try {
            ASCII85InputStream(byteIn).use { asciiIn ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var n: Int
                while ((asciiIn.read(buffer).also { n = it }) != -1) {
                    out.write(buffer, 0, n)
                }
                return out.toByteArray()
            }
        } catch (e: IOException) {
            throw IllegalArgumentException("Failed to decode ASCII85 string", e)
        }
    }

    private fun encodeToBase85(bytes: ByteArray): String {
        try {
            ByteArrayOutputStream().use { byteOut ->
                ASCII85OutputStream(byteOut).use { asciiOut ->
                    asciiOut.write(bytes)
                    asciiOut.flush()
                    return byteOut.toString(StandardCharsets.US_ASCII.name())
                }
            }
        } catch (e: IOException) {
            throw IllegalArgumentException("Failed to encode bytes to ASCII85", e)
        }
    }
}