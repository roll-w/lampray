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

    override fun <T> get(specification: SettingSpecification<T>): T? {
        return readRaw(specification) ?: specification.defaultValue
    }

    override fun <T> get(
        specification: SettingSpecification<T>,
        defaultValue: T
    ): T {
        return readRaw(specification) ?: defaultValue
    }

    private fun <T> readRaw(specification: SettingSpecification<T>): T? {
        val value = readKey(specification.keyName) ?: return null
        return specification.key.type.parse(value)
    }

    override fun <T> getValue(specification: SettingSpecification<T>): ConfigValue<T> {
        val value = get(specification)
        return SnapshotConfigValue(value, SettingSource.LOCAL, specification)
    }

    override fun list(specifications: List<SettingSpecification<*>>): List<ConfigValue<*>> {
        return specifications.map { getValue(it) }
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