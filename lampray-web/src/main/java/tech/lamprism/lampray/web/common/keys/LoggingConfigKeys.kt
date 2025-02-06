/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.web.common.keys

import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * @author RollW
 */
object LoggingConfigKeys : SettingSpecificationSupplier {
    const val LOGGING_PATH_CONSOLE = "[console]"

    // TODO: may support database setting source
    @JvmField
    val LOGGING_FILE_PATH =
        SettingSpecificationBuilder(SettingKey.ofString("logging.file.path"))
            .setTextDescription("The path of the log file, set the value to '[console]' to disable file logging.")
            .setDefaultValue("logs")
            .setRequired(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .build()

    @JvmField
    val LOGGING_FILE_MAX_SIZE =
        SettingSpecificationBuilder(SettingKey.ofLong("logging.file.max-size"))
            .setTextDescription("The maximum size of the log file.")
            .setDefaultValue(10 * 1024 * 1024) // = 10MB
            .setRequired(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .build()

    @JvmField
    val LOGGING_FILE_MAX_HISTORY =
        SettingSpecificationBuilder(SettingKey.ofInt("logging.file.max-history"))
            .setTextDescription("The maximum history of the log file.")
            .setDefaultValue(7)
            .setRequired(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .build()

    @JvmField
    val LOGGING_FILE_TOTAL_SIZE_CAP =
        SettingSpecificationBuilder(SettingKey.ofLong("logging.file.total-size-cap"))
            .setTextDescription("The total size cap of all log files.")
            .setDefaultValue(1024 * 1024 * 1024) // = 1GB
            .setRequired(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .build()

    /**
     * Logging level for loggers, for example:
     *
     * ```
     * logging.level=logger1:info, logger2:debug,\
     *   logger3:warn, logger4:error
     * ```
     *
     * The value is a comma-separated list of logger name and level pairs.
     */
    @JvmField
    val LOGGING_LEVEL =
        SettingSpecificationBuilder(SettingKey.ofString("logging.level"))
            .setTextDescription("""
                Logging level for loggers, for example:
                
                ```
                logging.level=logger1:info, logger2:debug,\
                  logger3:warn, logger4:error
                ```
                
                The value is a comma-separated list of logger name and level pairs.
            """.trimIndent())
            .setDefaultValue("")
            .setRequired(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .build()


    private val KEYS = listOf(
        LOGGING_FILE_PATH,
        LOGGING_FILE_MAX_SIZE,
        LOGGING_FILE_MAX_HISTORY,
        LOGGING_FILE_TOTAL_SIZE_CAP,
        LOGGING_LEVEL
    )

    @JvmStatic
    fun parseLoggingLevel(level: String?): Map<String, String> {
        // TODO: move to logging parser when setting value parser api is ready
        if (level == null) {
            return emptyMap()
        }
        return level.split(",").mapNotNull {
            if (it.isBlank()) {
                // skip for empty line
                return@mapNotNull null
            }

            val pair = it.trim().split(":")
            if (pair.size != 2) {
                throw IllegalArgumentException("Invalid logging level format '$it'")
            }
            val key = pair[0].trim()
            val value = pair[1].trim()
            if (key.isEmpty()) return@mapNotNull null
            if (value.isEmpty()) {
                throw IllegalArgumentException("Logging level cannot be empty: $it")
            }
            key to value
        }.toMap()
    }

    override val specifications: List<AttributedSettingSpecification<*, *>>
        get() = KEYS
}