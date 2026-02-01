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
package tech.lamprism.lampray.content.article.service

import org.springframework.stereotype.Service
import space.lingu.NonNull
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentOperator
import tech.lamprism.lampray.content.ContentProvider
import tech.lamprism.lampray.content.ContentTrait
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.article.Article
import tech.lamprism.lampray.content.article.persistence.ArticleDo.Companion.toDo
import tech.lamprism.lampray.content.article.persistence.ArticleRepository
import tech.lamprism.lampray.content.common.ContentErrorCode
import tech.lamprism.lampray.content.common.ContentException
import tech.lamprism.lampray.content.service.ContentMetadataService

/**
 * @author RollW
 */
@Service
class ArticleContentProvider(
    private val articleRepository: ArticleRepository,
    override val contentMetadataService: ContentMetadataService
) : ContentProvider,
    ArticleOperatorDelegate {
    override fun supports(@NonNull contentType: ContentType): Boolean {
        return contentType == ContentType.ARTICLE
    }

    override fun getContentDetails(@NonNull contentTrait: ContentTrait): ContentDetails {
        return articleRepository.findById(contentTrait.contentId).orElse(null)?.lock()
            ?: throw ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND)
    }

    override fun getContentOperator(@NonNull contentTrait: ContentTrait, checkDelete: Boolean): ContentOperator {
        val article = articleRepository.findById(contentTrait.contentId).orElse(null)
            ?: throw ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND)

        return ArticleOperatorImpl(article.lock(), this, checkDelete)
    }

    override fun getContentDetails(contentTraits: List<ContentTrait>): List<ContentDetails> {
        if (contentTraits.isEmpty()) {
            return emptyList()
        }
        return articleRepository
            .findAllById(contentTraits.map {
                it.contentId
            })
            .map {
                it.lock()
            }

    }

    override fun updateArticle(article: Article) {
        articleRepository.save(article.toDo())
    }
}
