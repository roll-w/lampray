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

package tech.lamprism.lampray.setting.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import tech.lamprism.lampray.setting.ConfigPath
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.setting.ConfigReader
import tech.lamprism.lampray.setting.ConfigValue
import tech.lamprism.lampray.setting.ConfigValueInfo
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecification
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import tech.lamprism.lampray.setting.SettingSpecificationProvider
import tech.lamprism.lampray.setting.SnapshotConfigValue
import tech.lamprism.lampray.setting.withStringCodec
import tech.lamprism.lampray.setting.data.SystemSettingDo
import tech.lamprism.lampray.setting.data.SystemSettingRepository

/**
 * System setting configuration provider that stores settings in the database.
 *
 * This provider uses different storage strategies based on type:
 * - Basic types (Int, Long, Float, Double, Boolean, String): Store as plain String using String codec
 * - Complex types: Store as JSON string using Jackson ObjectMapper
 *
 * @param objectMapper Jackson ObjectMapper for JSON serialization
 * @author RollW
 */
@Service
class SystemSettingConfigProvider(
    private val systemSettingRepository: SystemSettingRepository,
    private val settingSpecificationProvider: SettingSpecificationProvider,
    private val objectMapper: ObjectMapper
) : ConfigProvider {

    override val metadata: ConfigReader.Metadata =
        ConfigReader.Metadata(
            name = "SystemSettingConfigProvider",
            paths = listOf(ConfigPath("system_setting", SettingSource.DATABASE)),
        )

    override fun <T> get(specification: SettingSpecification<T>): T? {
        val setting = systemSettingRepository.findByKey(specification.keyName)
            .orElse(null) ?: return null
        val rawValue = setting.value ?: return null

        return parseValue(rawValue, specification)
    }

    override fun <T> get(
        specification: SettingSpecification<T>,
        defaultValue: T
    ): T = get(specification) ?: defaultValue

    override fun <T> getValue(specification: SettingSpecification<T>): ConfigValue<T> {
        val setting = systemSettingRepository.findByKey(specification.keyName)
            .orElse(null) ?: return SnapshotConfigValue(null, SettingSource.DATABASE, specification)
        val rawValue = setting.value ?: return SnapshotConfigValue(null, SettingSource.DATABASE, specification)

        return SnapshotConfigValue(
            parseValue(rawValue, specification),
            SettingSource.DATABASE,
            specification
        )
    }

    override fun list(specifications: List<SettingSpecification<*>>): List<ConfigValue<*>> {
        if (specifications.isEmpty()) {
            return emptyList()
        }
        val keys = specifications.map { it.keyName }.toSet()
        val settings = systemSettingRepository.findByKeyIn(keys)
            .associateBy { it.key }
        @Suppress("UNCHECKED_CAST")
        return (specifications as List<SettingSpecification<Any>>).map { spec ->
            val setting = settings[spec.keyName] ?: return@map SnapshotConfigValue(null, SettingSource.DATABASE, spec)
            val rawValue = setting.value ?: return@map SnapshotConfigValue(null, SettingSource.DATABASE, spec)

            ConfigValueInfo.from(
                specification = spec,
                value = parseValue(rawValue, spec),
                source = SettingSource.DATABASE,
                rawValue = setting.value,
                lastModified = setting.updateTime
            )
        }
    }

    override fun <T> set(spec: SettingSpecification<T>, value: T?): SettingSource {
        val setting = systemSettingRepository.findByKey(spec.keyName)
            .orElse(null)

        val stringValue = if (value == null) {
            null
        } else {
            formatValue(value, spec)
        }

        if (setting != null) {
            setting.value = stringValue
            systemSettingRepository.save(setting)
            return SettingSource.DATABASE
        }

        val newSetting = SystemSettingDo(
            key = spec.key.name,
            value = stringValue
        )
        systemSettingRepository.save(newSetting)
        return SettingSource.DATABASE
    }

    override fun <T> reset(spec: SettingSpecification<T>): SettingSource {
        val setting = systemSettingRepository.findByKey(spec.keyName)
            .orElse(null) ?: return SettingSource.NONE
        systemSettingRepository.delete(setting)
        return SettingSource.DATABASE
    }

    override fun supports(key: String): Boolean {
        return try {
            settingSpecificationProvider.getSettingSpecification(key)
                .supportedSources
                .contains(SettingSource.DATABASE)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parse stored string value to target type.
     *
     * Strategy:
     * 1. Try String codec for basic types
     * 2. Fall back to JSON deserialization for complex types
     */
    private fun <T> parseValue(rawValue: String, specification: SettingSpecification<T>): T {
        val configType = specification.key.type
        val typeWithStringCodec = configType.withStringCodec()

        // Try String codec first for basic types
        if (typeWithStringCodec[String::class.java] != null) {
            return try {
                typeWithStringCodec.parse(rawValue)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to parse value for key ${specification.keyName} as ${configType.targetClass}",
                    e
                )
            }
        }

        // Fall back to JSON deserialization for complex types
        return try {
            objectMapper.readValue(rawValue, configType.targetClass)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to deserialize JSON value for key ${specification.keyName} as ${configType.targetClass}",
                e
            )
        }
    }

    /**
     * Format value to string for storage.
     *
     * Strategy:
     * 1. Try String codec for basic types
     * 2. Fall back to JSON serialization for complex types
     */
    private fun <T> formatValue(value: T, specification: SettingSpecification<T>): String {
        val configType = specification.key.type
        val typeWithStringCodec = configType.withStringCodec()

        // Try String codec first for basic types
        if (typeWithStringCodec[String::class.java] != null) {
            return try {
                typeWithStringCodec.format(value, String::class.java)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to format value for key ${specification.keyName}",
                    e
                )
            }
        }

        // Fall back to JSON serialization for complex types
        return try {
            objectMapper.writeValueAsString(value)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to serialize value for key ${specification.keyName} to JSON",
                e
            )
        }
    }
}