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

package tech.lamprism.lampray.setting

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.toml.TomlReadFeature
import com.google.common.base.Strings
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * @author RollW
 */
class TomlConfigReader(
    inputStream: InputStream,
    private val path: String = ""
) : ConfigReader {
    private val values: MutableMap<String, Any> = hashMapOf()

    init {
        loadToml(inputStream)
        inputStream.close()
    }

    private val _metadata = ConfigReader.Metadata(
        name = "TomlConfigReader",
        ConfigPath(path, SettingSource.LOCAL)
    )

    override val metadata: ConfigReader.Metadata
        get() = _metadata

    private fun loadToml(inputStream: InputStream) {
        values.clear()
        tomlMapper.configure(TomlReadFeature.PARSE_JAVA_TIME, false)
        val readValue = tomlMapper.readValue(inputStream, Map::class.java)
        values.putAll(readValue as Map<String, Any>)
    }

    private fun readKey(key: String): Any? {
        if (key.isEmpty()) {
            return null
        }
        if (key in values) {
            return values[key]
        }
        val parts = key.split('.')
        var currentMap: Map<*, *> = values
        for (part in parts) {
            if (part !in currentMap) {
                return null
            }

            val value = currentMap[part]
            if (value is Map<*, *>) {
                currentMap = value
            } else {
                return value
            }
        }

        return null
    }

    override fun get(key: String): String? {
        val value = readKey(key) ?: return null
        return when (value) {
            is String -> value
            else -> value.toString()
        }
    }

    override fun get(key: String, defaultValue: String?): String? {
        return get(key) ?: defaultValue
    }

    override fun <T, V> get(specification: SettingSpecification<T, V>): T? {
        return readRaw(specification) ?: specification.defaultValue
    }

    override fun <T, V> get(
        specification: SettingSpecification<T, V>,
        defaultValue: T
    ): T {
        return readRaw(specification) ?: defaultValue
    }

    private fun <T, V> readRaw(specification: SettingSpecification<T, V>): T? {
        val value = readKey(specification.keyName) ?: return null
        return convertToType(value, specification)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T, V> convertToType(value: Any?, specification: SettingSpecification<T, V>): T? {
        // Converts value to the type specified in SettingSpecification
        if (value == null) {
            return null
        }
        return when (specification.key.type) {
            SettingType.STRING -> if (value is String) value as T else value.toString() as T?
            SettingType.INT -> when (value) {
                is Number -> value.toInt() as T
                is String -> value.toIntOrNull() as T?
                else -> null
            }

            SettingType.LONG -> when (value) {
                is Number -> value.toLong() as T
                is String -> value.toLongOrNull() as T?
                else -> null
            }

            SettingType.FLOAT -> when (value) {
                is Number -> value.toFloat() as T
                is String -> value.toFloatOrNull() as T?
                else -> null
            }

            SettingType.DOUBLE -> when (value) {
                is Number -> value.toDouble() as T
                is String -> value.toDoubleOrNull() as T?
                else -> null
            }

            SettingType.BOOLEAN -> when (value) {
                is Boolean -> value as T
                is String -> value.toBooleanStrictOrNull() as T?
                else -> null
            }

            SettingType.STRING_SET -> when (value) {
                is List<*> -> value.mapNotNull {
                    when (it) {
                        null -> null
                        is String -> it.trim()
                        else -> it.toString()
                    }
                }.toSet() as T
                is String -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet() as T
                else -> null
            }

            else -> throw IllegalArgumentException("Unsupported type: ${specification.key.type}")
        }
    }

    override fun <T, V> getValue(specification: SettingSpecification<T, V>): ConfigValue<T, V> {
        val value = get(specification)
        return SnapshotConfigValue(value, SettingSource.LOCAL, specification)
    }

    override fun list(): List<RawSettingValue> {
        return flattenMap(values)
    }

    private fun flattenMap(
        map: Map<String, Any>,
        prefix: String = ""
    ): List<RawSettingValue> {
        val result = mutableListOf<RawSettingValue>()
        for ((key, value) in map) {
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is Map<*, *> -> {
                    result.addAll(flattenMap(value as Map<String, Any>, fullKey))
                }

                is String -> {
                    result.add(RawSettingValue(fullKey, value, SettingSource.LOCAL))
                }

                else -> {
                    result.add(RawSettingValue(fullKey, value.toString(), SettingSource.LOCAL))
                }
            }
        }
        return result
    }

    companion object {
        private val tomlMapper = TomlMapper()

        @JvmStatic
        @JvmOverloads
        @Throws(IOException::class)
        fun loadConfig(
            appClz: Class<*>,
            path: String? = null,
            allowFail: Boolean = true
        ): TomlConfigReader {
            val (inputStream, path) = openConfigInput(appClz, path, allowFail)
            if (inputStream == null) {
                return TomlConfigReader(InputStream.nullInputStream(), path)
            }
            return TomlConfigReader(inputStream, path)
        }

        private fun openConfigInput(
            appClz: Class<*>,
            path: String?,
            allowFail: Boolean
        ): Pair<InputStream?, String> {
            val confFile = tryFile(path, allowFail)
            if (!confFile.exists()) {
                return Pair(
                    appClz.getResourceAsStream("/lampray.toml"),
                    "Default"
                )
            }
            return Pair(
                FileInputStream(confFile),
                confFile.absolutePath
            )
        }

        private fun tryFile(path: String?, allowFail: Boolean): File {
            if (!Strings.isNullOrEmpty(path)) {
                val givenFile = File(path!!)
                if (givenFile.exists()) {
                    return givenFile
                }
                if (!allowFail) {
                    throw FileNotFoundException(
                        "Given config file '$path' (absolute path: ${givenFile.absolutePath}) does not exist."
                    )
                }
            }
            val confFile = File("conf/lampray.toml")
            if (confFile.exists()) {
                return confFile
            }
            return File("lampray.toml")
        }
    }
}