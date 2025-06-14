/*
 * Copyright (C) 2023-2025 RollW
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

import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentOperator
import tech.lamprism.lampray.content.ContentProvider
import tech.lamprism.lampray.content.ContentTrait
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.comment.Comment
import tech.lamprism.lampray.content.comment.persistence.CommentDo.Companion.toDo
import tech.lamprism.lampray.content.comment.persistence.CommentRepository
import tech.lamprism.lampray.content.common.ContentErrorCode
import tech.lamprism.lampray.content.common.ContentException
import tech.lamprism.lampray.content.service.ContentMetadataService

/**
 * @author RollW
 */
@Service
class CommentContentProvider(
    private val commentRepository: CommentRepository,
    override val contentMetadataService: ContentMetadataService
) : ContentProvider, CommentOperatorDelegate {
    override fun supports(contentType: ContentType): Boolean {
        return contentType == ContentType.COMMENT
    }

    override fun getContentOperator(
        contentTrait: ContentTrait,
        checkDelete: Boolean
    ): ContentOperator {
        val commentDo = commentRepository
            .findById(contentTrait.contentId)
            .orElseThrow {
                ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND, "Comment not found")
            }
        return CommentOperatorImpl(commentDo.lock(), this, checkDelete)
    }

    override fun getContentDetails(contentTraits: List<ContentTrait>): List<ContentDetails> {
        if (contentTraits.isEmpty()) {
            return emptyList()
        }
        return commentRepository.findAllById(contentTraits.map {
            it.contentId
        }).map {
            it.lock()
        }
    }

    override fun updateComment(comment: Comment) {
        commentRepository.save(comment.toDo())
    }
}