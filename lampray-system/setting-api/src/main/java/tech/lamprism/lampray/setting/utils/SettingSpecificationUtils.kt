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

package tech.lamprism.lampray.setting.utils

import org.apache.commons.text.WordUtils
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingDescriptionProvider
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import tech.lamprism.lampray.setting.SettingType
import java.util.SortedMap
import kotlin.math.min

object SettingSpecificationUtils {
    private const val LENGTH = 120

    private data class Table(
        val key: String,
        val directives: MutableList<AttributedSettingSpecification<*, *>>,
    )

    private val keyComparator: Comparator<String> = Comparator { o1, o2 ->
        val partsA = o1.split('.')
        val partsB = o2.split('.')
        val minSize = min(partsA.size, partsB.size)
        for (i in 0 until minSize) {
            val partCompare = partsA[i].compareTo(partsB[i])
            if (partCompare != 0) {
                return@Comparator partCompare
            }
        }
        partsA.size - partsB.size
    }

    private fun Collection<AttributedSettingSpecification<*, *>>.asTables(): Map<String, Table> {
        val tables: SortedMap<String, Table> = sortedMapOf(keyComparator)
        this.sortedWith { o1, o2 ->
            return@sortedWith keyComparator.compare(o1.keyName, o2.keyName)
        }.forEach { specification ->
            val tableKey = specification.keyName.substringBeforeLast('.')
            if (tableKey !in tables) {
                tables[tableKey] = Table(
                    key = tableKey,
                    directives = mutableListOf(specification)
                )
                return@forEach
            }
            tables[tableKey]?.directives += specification
        }

        return tables
    }

    private fun encodeTomlValue(value: Any?): String {
        return when (value) {
            is String -> "\"${escapeTomlString(value)}\""
            is Boolean -> value.toString()
            is Number -> value.toString()
            is Collection<*> -> value.joinToString(prefix = "[", postfix = "]") { encodeTomlValue(it) }
            is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}", separator = ", ") { (k, v) ->
                "$k = ${encodeTomlValue(v)}"
            }

            else -> throw IllegalArgumentException("Unsupported value type: ${value?.javaClass?.name}")
        }
    }

    private fun escapeTomlString(value: String): String {
        val builder = StringBuilder()
        for (c in value) {
            when (c) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\t' -> builder.append("\\t")
                '\n' -> builder.append("\\n")
                '\u000c' -> builder.append("\\f")
                '\r' -> builder.append("\\r")
                else -> {
                    if (c.code <= 0x1F) {
                        // Use Unicode escape for other control characters
                        val hex = String.format("\\u%04x", c.code)
                        builder.append(hex)
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        return builder.toString()
    }

    @JvmStatic
    fun Collection<AttributedSettingSpecification<*, *>>.formatAsToml(
        settingDescriptionProvider: SettingDescriptionProvider
    ): String {
        val tables = this.asTables()

        return tables.map { (key, table) ->
            val directives = table.directives
            buildString {
                if (key.isNotEmpty()) {
                    append("[$key]\n")
                }
            } + directives.joinToString("\n\n") { it ->
                val desc = settingDescriptionProvider.getSettingDescription(it.description)
                val comment = if (desc.isEmpty()) "" else
                    desc.lines().flatMap { it.wrap(LENGTH).lineSequence() }
                        .joinToString(separator = "\n") { line ->
                            line.addCommentMark()
                        } + "\n"
                val keyName = it.keyName.removePrefix(key).substringAfter('.')
                val typeHint = buildString {
                    append("Type: ${it.key.type.toReadableName()}")
                    append(
                        if (it.isRequired) " [Required]" else
                            " [Optional]"
                    )
                    append(
                        if (it.allowAnyValue()) " (No value restrictions)"
                        else " (Allowed: ${it.valueEntries.joinToString(", ")})"
                    )
                }.wrap(LENGTH).lineSequence()
                    .joinToString(separator = "\n") { line ->
                        line.addCommentMark()
                    } + "\n"

                buildString {
                    if (comment.isNotEmpty()) {
                        append(comment)
                        append("#\n")
                    }
                    append(typeHint)
                    append("$keyName = ")
                    append(encodeTomlValue(it.defaultValue ?: ""))
                }
            }
        }.joinToString("\n\n")
    }

    private fun <T, V> SettingType<T, V>.toReadableName(): String {
        return when (this) {
            SettingType.STRING -> "String"
            SettingType.INT -> "Integer"
            SettingType.BOOLEAN -> "Boolean"
            SettingType.FLOAT -> "Float"
            SettingType.DOUBLE -> "Double"
            SettingType.LONG -> "Long"
            SettingType.STRING_SET -> "String List"
            else -> {
                throw IllegalArgumentException("Unsupported setting type: $this")
            }
        }
    }

    private fun String.addCommentMark() = "# $this"

    private fun String.wrap(length: Int, wrapLongWords: Boolean = true) =
        WordUtils.wrap(this, length, null, wrapLongWords)
}
