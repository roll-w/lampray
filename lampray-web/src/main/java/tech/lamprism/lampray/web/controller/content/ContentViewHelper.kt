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
package tech.lamprism.lampray.web.controller.content

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.web.controller.article.model.ArticleVo
import tech.lamprism.lampray.web.controller.comment.model.CommentVo
import tech.lamprism.lampray.web.controller.content.vo.ContentVo

/**
 * @author RollW
 */
object ContentViewHelper {
    @JvmStatic
    fun toContentView(details: ContentDetails): ContentVo? {
        return when (details.getContentType()) {
            ContentType.ARTICLE -> ArticleVo.of(details)
            ContentType.COMMENT -> CommentVo.of(details)
            else -> throw IllegalStateException("Unexpected value: " + details.getContentType())
        }
    }
}
