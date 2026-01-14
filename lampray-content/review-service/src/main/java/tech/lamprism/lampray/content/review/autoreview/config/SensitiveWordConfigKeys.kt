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
package tech.lamprism.lampray.content.review.autoreview.config

import org.springframework.stereotype.Component
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingDescription
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier


/**
 * @author RollW
 */
@Component
object SensitiveWordConfigKeys : SettingSpecificationSupplier {

    /**
     * Path to the sensitive words file. Each line in the file represents a sensitive word.
     * If not specified, uses inline words configuration.
     */
    @JvmField
    val SENSITIVE_WORD_FILE_PATH =
        SettingSpecificationBuilder(SettingKey.ofString("review.auto-review.sensitive-word.file-path"))
            .setDescription(
                SettingDescription.text(
                    "Path to the sensitive words file. Each line in the file represents a sensitive word. " +
                            "If not specified, uses inline words configuration from 'review.sensitive-word.words'."
                )
            )
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setAllowAnyValue(true)
            .build()

    /**
     * Inline list of sensitive words. Used when file-path is not specified.
     */
    @JvmField
    val SENSITIVE_WORD_LIST =
        SettingSpecificationBuilder(SettingKey.ofStringSet("review.auto-review.sensitive-word.words"))
            .setDescription(
                SettingDescription.text(
                    "Inline list of sensitive words. Used when file-path is not specified. " +
                            "Each entry represents a sensitive word or phrase to detect."
                )
            )
            .setSupportedSources(SettingSource.VALUES)
            .setDefaultValue("")
            .build()

    /**
     * Maximum window size for cross-node detection.
     */
    @JvmField
    val MAX_WINDOW_SIZE =
        SettingSpecificationBuilder(SettingKey.ofInt("review.auto-review.sensitive-word.max-window-size"))
            .setDescription(
                SettingDescription.text(
                    "Maximum window size for cross-node detection. " +
                            "This determines how many characters to keep in the buffer when detecting " +
                            "sensitive words that may span across multiple text nodes."
                )
            )
            .setSupportedSources(SettingSource.VALUES)
            .setDefaultValue(50)
            .build()

    override val specifications: List<AttributedSettingSpecification<*, *>>
        get() = listOf(
            SENSITIVE_WORD_FILE_PATH,
            SENSITIVE_WORD_LIST,
            MAX_WINDOW_SIZE
        )
}