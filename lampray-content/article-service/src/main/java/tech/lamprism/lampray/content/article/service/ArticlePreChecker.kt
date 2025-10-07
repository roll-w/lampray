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

import com.google.common.base.Strings
import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.UncreatedContent
import tech.lamprism.lampray.content.UncreatedContentPreChecker
import tech.lamprism.lampray.content.article.ArticleDetailsMetadata
import tech.lamprism.lampray.content.article.common.ArticleErrorCode
import tech.lamprism.lampray.content.common.ContentException

/**
 * @author RollW
 */
@Service
class ArticlePreChecker : UncreatedContentPreChecker {
    override fun checkUncreatedContent(uncreatedContent: UncreatedContent) {
        uncreatedContent.title.let { it ->
            if (Strings.isNullOrEmpty(it)) {
                throw ContentException(ArticleErrorCode.ERROR_TITLE_EMPTY)
            }
            if (it.length > 100) {
                throw ContentException(ArticleErrorCode.ERROR_TITLE_TOO_LONG)
            }
        }
        uncreatedContent.content.let { it ->
            if (it.isEmpty()) {
                throw ContentException(ArticleErrorCode.ERROR_CONTENT_EMPTY)
            }
//            if (it.length > 100000) {
//                throw ContentException(ArticleErrorCode.ERROR_CONTENT_TOO_LONG)
//            }
            // TODO: more checks
        }
        val metadata = uncreatedContent.metadata
        if (metadata !is ArticleDetailsMetadata) {
            throw IllegalArgumentException("Metadata should be ArticleDetailsMetadata")
        }
    }

    override fun supports(contentType: ContentType): Boolean {
        return contentType == ContentType.ARTICLE
    }
}