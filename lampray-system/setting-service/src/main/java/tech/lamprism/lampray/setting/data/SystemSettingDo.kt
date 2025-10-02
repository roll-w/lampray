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

package tech.lamprism.lampray.setting.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.setting.SystemSettingResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "system_setting", uniqueConstraints = [
    UniqueConstraint(columnNames = ["key"], name = "index__key")
])
class SystemSettingDo(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "key")
    var key: String = "",

    @Column(name = "value")
    var value: String? = null,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : DataEntity<Long> {
    override fun getId(): Long? = id

    fun setId(id: Long?) {
        this.id = id
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getCreateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime = updateTime

    override fun getSystemResourceKind(): SystemResourceKind =
        SystemSettingResourceKind
}