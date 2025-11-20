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
package tech.lamprism.lampray.content.comment.service

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentDetailsMetadata
import tech.lamprism.lampray.content.ContentOperator
import tech.lamprism.lampray.content.comment.Comment
import tech.lamprism.lampray.content.service.AbstractContentOperator
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.rollw.common.web.CommonRuntimeException
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class CommentOperatorImpl internal constructor(
    private var comment: Comment,
    private val delegate: CommentOperatorDelegate,
    checkDeleted: Boolean
) : AbstractContentOperator(comment, delegate.contentMetadataService, checkDeleted),
    ContentOperator {

    private val commentBuilder: Comment.Builder = comment.toBuilder()

    override fun updateContent(): ContentDetails {
        comment = commentBuilder
            .setUpdateTime(OffsetDateTime.now())
            .build()
        delegate.updateComment(comment)
        return comment
    }

    @Throws(CommonRuntimeException::class)
    override fun setNameInternal(name: String?): Boolean {
        throw UnsupportedOperationException()
    }

    @Throws(CommonRuntimeException::class)
    override fun setContentInternal(content: StructuralText): Boolean {
        throw UnsupportedOperationException()
    }

    @Throws(CommonRuntimeException::class)
    override fun setMetadataInternal(metadata: ContentDetailsMetadata?): Boolean {
        return false
    }
}
