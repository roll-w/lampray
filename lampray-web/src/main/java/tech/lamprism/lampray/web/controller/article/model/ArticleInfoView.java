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

package tech.lamprism.lampray.web.controller.article.model;

import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentDetailsMetadata;
import tech.lamprism.lampray.content.ContentAccessAuthType;
import tech.lamprism.lampray.content.ContentMetadataDetails;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.article.ArticleDetailsMetadata;
import tech.lamprism.lampray.web.controller.content.vo.ContentVo;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public record ArticleInfoView(
        long id,
        long userId,
        String title,
        String cover,
        OffsetDateTime createTime,
        OffsetDateTime updateTime,
        ContentAccessAuthType accessAuthType
) implements ContentVo {
    public static ArticleInfoView from(ContentDetails contentDetails) {
        if (contentDetails == null) {
            return null;
        }
        if (contentDetails.getContentType() != ContentType.ARTICLE) {
            throw new IllegalArgumentException("Content details is not an article.");
        }
        ContentDetailsMetadata metadata = contentDetails.getMetadata();
        if (!(metadata instanceof ArticleDetailsMetadata articleMetadata)) {
            return new ArticleInfoView(
                    contentDetails.getContentId(),
                    contentDetails.getUserId(),
                    contentDetails.getTitle(),
                    null,
                    contentDetails.getCreateTime(),
                    contentDetails.getUpdateTime(),
                    resolveAccessAuthType(contentDetails)
            );
        }
        return new ArticleInfoView(
                contentDetails.getContentId(),
                contentDetails.getUserId(),
                contentDetails.getTitle(),
                articleMetadata.getCover(),
                contentDetails.getCreateTime(),
                contentDetails.getUpdateTime(),
                resolveAccessAuthType(contentDetails)
        );
    }

    private static ContentAccessAuthType resolveAccessAuthType(ContentDetails contentDetails) {
        if (contentDetails instanceof ContentMetadataDetails<?> metadataDetails) {
            return metadataDetails.getContentAccessAuthType();
        }
        return ContentAccessAuthType.PUBLIC;
    }
}
