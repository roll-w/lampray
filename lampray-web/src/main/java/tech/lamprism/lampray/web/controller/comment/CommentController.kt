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
package tech.lamprism.lampray.web.controller.comment

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import tech.lamprism.lampray.content.ContentPublishProvider
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.SimpleUncreatedContent
import tech.lamprism.lampray.content.collection.ContentCollectionType
import tech.lamprism.lampray.content.comment.CommentDetailsMetadata
import tech.lamprism.lampray.web.common.ApiContext
import tech.lamprism.lampray.web.controller.Api
import tech.lamprism.lampray.web.controller.comment.model.CommentRequest
import tech.lamprism.lampray.web.controller.comment.model.CommentVo
import tech.rollw.common.web.HttpResponseEntity
import tech.rollw.common.web.ParameterFailedException
import tech.rollw.common.web.system.ContextThreadAware

/**
 * @author RollW
 */
@Api
class CommentController(
    private val apiContextThreadAware: ContextThreadAware<ApiContext>,
    private val contentPublishProvider: ContentPublishProvider
) {
    @PostMapping("/{contentType}/{contentId}/comments")
    fun createComment(
        @PathVariable("contentId") contentId: Long,
        @PathVariable("contentType") type: String,
        @RequestBody commentRequest: CommentRequest
    ): HttpResponseEntity<CommentVo> {
        val context = apiContextThreadAware.contextThread
            .context
        val user = context.user!!

        val contentType = getContentType(type)
        val commentDetailsMetadata = CommentDetailsMetadata(
            contentType, contentId,
            commentRequest.parent
        )
        val uncreatedContent = SimpleUncreatedContent(
            ContentType.COMMENT,
            user,
            null,
            commentRequest.content,
            commentDetailsMetadata
        )
        val contentDetails = contentPublishProvider.publishContent(
            uncreatedContent
        )
        return HttpResponseEntity.success(
            CommentVo.of(contentDetails)
        )
    }

    // TODO: implement the following methods
    @GetMapping("/{contentType}/{contentId}/comments")
    fun getComments(
        @PathVariable("contentId") contentId: Long?,
        @PathVariable("contentType") type: String
    ): HttpResponseEntity<List<CommentVo>> {
        val contentType = getContentType(type)
        val collectionType = getFromContentType(contentType)

        return HttpResponseEntity.success(listOf())
    }


    @GetMapping("/user/comments")
    fun getCommentsOfCurrentUser(): HttpResponseEntity<List<CommentVo>> {
        return HttpResponseEntity.success(listOf())
    }

    @GetMapping("/users/{userId}/comments")
    fun getCommentsOfUser(@PathVariable("userId") userId: Long): HttpResponseEntity<List<CommentVo>> {
        return HttpResponseEntity.success(listOf())
    }

    private fun getFromContentType(contentType: ContentType): ContentCollectionType {
        return when (contentType) {
            ContentType.ARTICLE -> ContentCollectionType.ARTICLE_COMMENTS
            ContentType.POST -> ContentCollectionType.POST_COMMENTS
            ContentType.COMMENT -> ContentCollectionType.COMMENTS
            else -> throw IllegalArgumentException("Unsupported content type: $contentType")
        }
    }

    companion object {
        private fun getContentType(contentType: String): ContentType =
            ContentType.findByName(contentType)
                ?: throw ParameterFailedException("Content type not found.")

    }
}
