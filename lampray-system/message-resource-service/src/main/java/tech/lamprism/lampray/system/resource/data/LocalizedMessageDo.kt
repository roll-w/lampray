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

package tech.lamprism.lampray.system.resource.data

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.common.data.LocaleAttributeConverter
import tech.lamprism.lampray.system.resource.LocalizedMessage
import tech.lamprism.lampray.system.resource.LocalizedMessageResource
import tech.lamprism.lampray.system.resource.LocalizedMessageResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime
import java.util.Locale

/**
 * @author RollW
 */
@Entity
@Table(
    name = "localized_message", uniqueConstraints = [
        UniqueConstraint(columnNames = ["key", "locale"], name = "index__key_locale")
    ]
)
class LocalizedMessageDo(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "key", nullable = false, length = 255)
    override var key: String = "",

    @Lob
    @Column(name = "value", nullable = false, length = 20000000)
    override var value: String = "",

    @Column(name = "locale", nullable = false, length = 20)
    @Convert(converter = LocaleAttributeConverter::class)
    override var locale: Locale = Locale.getDefault(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : LocalizedMessageResource, DataEntity<Long> {
    override fun getEntityId(): Long? {
        return id
    }

    fun setId(id: Long?) {
        this.id = id
    }

    override fun getCreateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getSystemResourceKind(): SystemResourceKind {
        return LocalizedMessageResourceKind
    }

    fun lock(): LocalizedMessage {
        return LocalizedMessage(id, key, value, locale, updateTime)
    }

    companion object {
        fun LocalizedMessage.toDo(): LocalizedMessageDo {
            return LocalizedMessageDo(entityId, key, value, locale, updateTime)
        }
    }
}