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

package tech.lamprism.lampray.setting

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

/**
 * @author RollW
 */
class MessageSourceSettingDescriptionProvider(
    private val messageSource: MessageSource
) : SettingDescriptionProvider {

    override fun getSettingDescription(description: SettingDescription): String {
        return when (description) {
            is SettingDescription.Text -> description.value
            is SettingDescription.Resource -> getResourceValue(description.value)
        }
    }

    private fun getResourceValue(resource: String): String {
        val locale = LocaleContextHolder.getLocale()
        return try {
            messageSource.getMessage(
                resource,
                null,
                locale
            )
        } catch (_: Exception) {
            resource
        }
    }
}