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
package tech.lamprism.lampray.web.domain.noticeboard

import space.lingu.NonNull
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.LongEntityBuilder
import tech.lamprism.lampray.web.domain.systembased.LampSystemResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * Notice board, system-wide announcement.
 *
 * @author RollW
 */
data class NoticeBoard(
    private val id: Long?,
    val title: String,
    val content: String?,
    /**
     * Creator. Needs to be ADMIN or EDITOR.
     */
    val userId: Long,
    private val createTime: OffsetDateTime,
    private val updateTime: OffsetDateTime
) : DataEntity<Long> {
    override fun getEntityId(): Long? {
        return id
    }

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    @NonNull
    override fun getSystemResourceKind(): SystemResourceKind = LampSystemResourceKind.SYSTEM_NOTICE_BOARD

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : LongEntityBuilder<NoticeBoard> {
        private var id: Long? = null
        private var title: String = ""
        private var content: String? = null
        private var userId: Long = 0
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        internal constructor(noticeboard: NoticeBoard) {
            this.id = noticeboard.id
            this.title = noticeboard.title
            this.content = noticeboard.content
            this.userId = noticeboard.userId
            this.createTime = noticeboard.createTime
            this.updateTime = noticeboard.updateTime
        }

        override fun setEntityId(id: Long?): Builder {
            this.id = id
            return this
        }

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setContent(content: String?): Builder {
            this.content = content
            return this
        }

        fun setUserId(userId: Long): Builder {
            this.userId = userId
            return this
        }

        fun setCreateTime(createTime: OffsetDateTime): Builder {
            this.createTime = createTime
            return this
        }

        fun setUpdateTime(updateTime: OffsetDateTime): Builder {
            this.updateTime = updateTime
            return this
        }

        override fun build(): NoticeBoard {
            return NoticeBoard(id, title, content, userId, createTime, updateTime)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
