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
package tech.lamprism.lampray.content.article.service

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentDetailsMetadata
import tech.lamprism.lampray.content.ContentOperator
import tech.lamprism.lampray.content.article.Article
import tech.lamprism.lampray.content.service.AbstractContentOperator
import tech.lamprism.lampray.content.structuraltext.StructuralText
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class ArticleOperatorImpl internal constructor(
    private var article: Article,
    private val delegate: ArticleOperatorDelegate,
    checkDeleted: Boolean
) : AbstractContentOperator(article, delegate.contentMetadataService, checkDeleted),
    ContentOperator {
    private val articleBuilder: Article.Builder = article.toBuilder()

    override fun setNameInternal(name: String?): Boolean {
        if (article.title == name) {
            return false
        }
        if (name == null) {
            return false
        }
        articleBuilder.setTitle(name)
        return true
    }

    override fun setContentInternal(content: StructuralText?): Boolean {
        if (article.content == content) {
            return false
        }
        articleBuilder.setContent(content)
        return true
    }

    override fun setMetadataInternal(metadata: ContentDetailsMetadata?): Boolean {
        if (article.metadata == metadata) {
            return false
        }
        // TODO: article metadata
        return true
    }

    override fun updateContent(): ContentDetails {
        val newArticle = articleBuilder
            .setUpdateTime(OffsetDateTime.now())
            .build()
        delegate.updateArticle(newArticle)
        return newArticle.also {
            article = it
        }
    }
}
