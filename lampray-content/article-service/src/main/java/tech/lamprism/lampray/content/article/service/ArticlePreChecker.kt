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
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextEmptyException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextNodeTooLongException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextRootNotDocumentException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextTooDeepException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextTooLongException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextTooShortException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextValidationException
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextValidator

/**
 * @author RollW
 */
@Service
class ArticlePreChecker : UncreatedContentPreChecker {
    private val structuralTextValidator = StructuralTextValidator(
        minTotalTextLength = 1,
        maxTotalTextLength = 100000,
    )

    override fun checkUncreatedContent(uncreatedContent: UncreatedContent) {
        val title = uncreatedContent.title
        if (Strings.isNullOrEmpty(title)) {
            throw ContentException(ArticleErrorCode.ERROR_TITLE_EMPTY)
        }
        if (title!!.length > 100) {
            throw ContentException(ArticleErrorCode.ERROR_TITLE_TOO_LONG)
        }

        // Validate content presence first
        val content = uncreatedContent.content ?: throw ContentException(ArticleErrorCode.ERROR_CONTENT_EMPTY)

        try {
            structuralTextValidator.validate(content)
        } catch (e: StructuralTextValidationException) {
            when (e) {
                is StructuralTextTooLongException -> {
                    throw ContentException(ArticleErrorCode.ERROR_CONTENT_TOO_LONG)
                }

                is StructuralTextTooShortException -> {
                    throw ContentException(ArticleErrorCode.ERROR_CONTENT_TOO_SHORT)
                }

                is StructuralTextEmptyException -> {
                    throw ContentException(ArticleErrorCode.ERROR_CONTENT_EMPTY)
                }

                is StructuralTextTooDeepException,
                is StructuralTextRootNotDocumentException,
                is StructuralTextNodeTooLongException -> {
                    // Node too long maps to content too long
                    throw ContentException(ArticleErrorCode.ERROR_CONTENT_TOO_LONG)
                }

                else -> {
                    // Fallback to generic article error
                    throw ContentException(ArticleErrorCode.ERROR_ARTICLE)
                }
            }
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