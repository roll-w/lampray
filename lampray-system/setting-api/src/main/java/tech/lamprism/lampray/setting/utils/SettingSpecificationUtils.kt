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

package tech.lamprism.lampray.setting.utils

import org.apache.commons.text.WordUtils
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingDescriptionProvider
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName

object SettingSpecificationUtils {
    private const val LENGTH = 100

    @JvmStatic
    fun Collection<AttributedSettingSpecification<*, *>>.formatAsConfigs(
        settingDescriptionProvider: SettingDescriptionProvider
    ): String = sortedBy { it -> it.keyName }
        .joinToString(separator = "\n") { specification ->
            val desc = settingDescriptionProvider.getSettingDescription(
                specification.description
            )
            val comment = if (desc.isEmpty()) "" else
                buildString {
                    append(desc.wrap(LENGTH))
                }.lines().joinToString(separator = "\n") { line ->
                    line.addCommentMark()
                } + "\n"
            val declaration = "${specification.keyName}=${specification.defaultValue ?: ""}"
            comment + declaration + "\n"
        }

    private fun String.addCommentMark() = "# $this"

    private fun String.wrap(length: Int, wrapLongWords: Boolean = true) =
        WordUtils.wrap(this, length, null, wrapLongWords)
}
