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

package tech.lamprism.lampray.web.controller.article.model;

import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentAccessAuthType;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentMetadataDetails;
import tech.lamprism.lampray.content.structuraltext.StructuralText;
import tech.lamprism.lampray.web.controller.content.vo.ContentVo;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public record ArticleVo(
        long id,
        String title,
        StructuralText content,
        long userId,
        OffsetDateTime createTime,
        OffsetDateTime updateTime,
        ContentAccessAuthType accessAuthType
        // TODO: add more fields
) implements ContentVo {

    public static ArticleVo of(ContentMetadataDetails<?> contentMetadataDetails) {
        ContentDetails contentDetails = contentMetadataDetails.getContentDetails();
        return new ArticleVo(
                contentDetails.getContentId(),
                contentDetails.getTitle(),
                contentDetails.getContent(),
                contentDetails.getUserId(),
                contentDetails.getCreateTime(),
                contentDetails.getUpdateTime(),
                contentMetadataDetails.getContentAccessAuthType()
        );
    }

    public static ArticleVo of(ContentDetails contentDetails) {
        if (contentDetails instanceof ContentMetadataDetails<?> contentMetadataDetails) {
            return of(contentMetadataDetails);
        }
        return new ArticleVo(
                contentDetails.getContentId(),
                contentDetails.getTitle(),
                contentDetails.getContent(),
                contentDetails.getUserId(),
                contentDetails.getCreateTime(),
                contentDetails.getUpdateTime(),
                ContentAccessAuthType.PUBLIC
        );
    }

    public static ArticleVo of(Content content) {
        if (content instanceof ContentDetails contentDetails) {
            return of(contentDetails);
        }
        return null;
    }
}
