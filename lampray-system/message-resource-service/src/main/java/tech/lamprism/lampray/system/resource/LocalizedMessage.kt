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
package tech.lamprism.lampray.system.resource

import space.lingu.NonNull
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.TimeAttributed
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Localized message resource, overrides the default message resource
 * in the system.
 *
 * Could be used to override the default i18n messages in the system.
 *
 * @author RollW
 */
data class LocalizedMessage(
    private val id: Long?,
    override val key: String,
    override val value: String,
    override val locale: Locale,
    private val updateTime: OffsetDateTime
) : LocalizedMessageResource, DataEntity<Long> {
    override fun getEntityId(): Long? = id

    override fun getCreateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime = updateTime

    @NonNull
    override fun getSystemResourceKind(): SystemResourceKind {
        return LocalizedMessageResourceKind
    }

    fun toBuilder() = Builder(this)

    class Builder {
        private var id: Long? = null
        private var key: String? = null
        private var value: String? = null
        private var locale: Locale? = null
        private var updateTime: OffsetDateTime? = null

        constructor()

        constructor(localizedMessage: LocalizedMessage) {
            this.id = localizedMessage.id
            this.key = localizedMessage.key
            this.value = localizedMessage.value
            this.locale = localizedMessage.locale
            this.updateTime = localizedMessage.updateTime
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setKey(key: String) = apply {
            this.key = key
        }

        fun setValue(value: String) = apply {
            this.value = value
        }

        fun setLocale(locale: Locale) = apply {
            this.locale = locale
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): LocalizedMessage =
            LocalizedMessage(id, key!!, value!!, locale!!, updateTime!!)
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()

        @JvmStatic
        fun of(
            key: String, value: String,
            locale: Locale
        ): LocalizedMessage {
            return builder()
                .setKey(key)
                .setValue(value)
                .setLocale(locale)
                .build()
        }
    }
}
