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
package tech.lamprism.lampray.web.controller.comment.model

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.comment.CommentDetailsMetadata
import tech.lamprism.lampray.web.controller.content.vo.ContentVo
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class CommentVo(
    val id: Long,
    val userId: Long,
    val parent: Long,
    val content: String,
    val contentId: Long,
    val contentType: ContentType,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime
) : ContentVo {

    override fun id(): Long = id

    override fun createTime(): OffsetDateTime = createTime

    override fun updateTime(): OffsetDateTime = updateTime

    companion object {
        @JvmStatic
        fun of(contentDetails: ContentDetails?): CommentVo? {
            if (contentDetails == null) {
                return null
            }
            val metadata = contentDetails.metadata
                ?: return null
            if (metadata !is CommentDetailsMetadata) {
                return null
            }
            return CommentVo(
                contentDetails.contentId,
                contentDetails.userId,
                metadata.parentId,
                contentDetails.content,
                metadata.contentId,
                metadata.contentType,
                contentDetails.createTime,
                contentDetails.updateTime
            )
        }
    }

}
