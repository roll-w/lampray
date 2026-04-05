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

package tech.lamprism.lampray.system.resource.service

import org.springframework.stereotype.Service
import tech.lamprism.lampray.common.data.ResourceIdGenerator
import tech.lamprism.lampray.system.resource.LocalizedMessageResource
import tech.lamprism.lampray.system.resource.LocalizedMessageResourceProvider
import tech.lamprism.lampray.system.resource.SimpleLocalizedMessageResource
import tech.lamprism.lampray.system.resource.data.LocalizedMessageEntity
import tech.lamprism.lampray.system.resource.data.LocalizedMessageRepository
import java.time.OffsetDateTime
import java.util.Locale
import java.util.function.Supplier

/**
 * @author RollW
 */
@Service
class LocalizedMessageResourceService(
    private val localizedMessageRepository: LocalizedMessageRepository,
    private val resourceIdGenerator: ResourceIdGenerator
) : LocalizedMessageResourceProvider {
    private var _fallbackLocale: Locale = Locale.ROOT

    override fun setFallbackLocale(locale: Locale) {
        _fallbackLocale = locale
    }

    override fun getFallbackLocale(): Locale = _fallbackLocale

    override fun setMessageResource(
        key: String,
        value: String,
        locale: Locale
    ) {
        val localizedMessageEntity = localizedMessageRepository
            .findByKey(key, locale).orElse(null) ?: LocalizedMessageEntity().apply {
            this.key = key
            this.value = value
            this.locale = locale
        }
        if (localizedMessageEntity.id != null &&
            localizedMessageEntity.value == value
        ) {
            return
        }
        val time = OffsetDateTime.now()
        localizedMessageEntity.apply {
            this.value = value
            this.updateTime = time
        }.let {
            localizedMessageRepository.save(it)
        }
    }

    override fun setMessageResource(messageResource: LocalizedMessageResource) {
        setMessageResource(
            messageResource.key,
            messageResource.value,
            messageResource.locale
        )
    }

    override fun removeMessageResource(key: String) {
        localizedMessageRepository.deleteByKey(key)
    }

    override fun removeMessageResource(key: String, locale: Locale) {
        localizedMessageRepository.deleteByKey(key, locale)
    }

    override fun getMessageResource(
        key: String,
        locale: Locale
    ): LocalizedMessageResource? {
        val localizedMessageEntity = localizedMessageRepository
            .findByKey(key, locale).orElse(null)
        return localizedMessageEntity?.lock()
    }

    override fun getMessageResource(
        key: String,
        locale: Locale,
        defaultValue: String
    ): LocalizedMessageResource? = getMessageResource(key, locale) {
        defaultValue
    }

    override fun getMessageResource(
        key: String,
        locale: Locale,
        defaultValueProvider: Supplier<String>
    ): LocalizedMessageResource? {
        val localizedMessageEntity = localizedMessageRepository
            .findByKey(key, locale).orElse(null)
        if (localizedMessageEntity != null) {
            return localizedMessageEntity.lock()
        }
        return SimpleLocalizedMessageResource(key, defaultValueProvider.get(), locale)
    }

    override fun getMessageResources(): List<LocalizedMessageResource> {
        return localizedMessageRepository.findAll().map { it.lock() }
    }

    override fun getMessageResources(key: String): List<LocalizedMessageResource> {
        return localizedMessageRepository.findByKey(key).map { it.lock() }
    }
}
