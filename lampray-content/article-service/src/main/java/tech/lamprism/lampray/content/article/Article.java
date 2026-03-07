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

package tech.lamprism.lampray.content.article;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.EntityBuilder;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentDetailsMetadata;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.structuraltext.StructuralText;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class Article implements DataEntity<String>, ContentDetails {
    private final Long id;
    private final String resourceId;
    private final long userId;
    private final String title;
    private final String cover;
    private final StructuralText content;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;

    public Article(Long id, String resourceId, long userId, String title, String cover,
                   StructuralText content, OffsetDateTime createTime, OffsetDateTime updateTime) {
        this.id = id;
        this.resourceId = resourceId;
        this.userId = userId;
        this.title = title;
        this.cover = cover;
        this.content = content;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getEntityId() {
        return resourceId;
    }

    @Override
    @NonNull
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    @Nullable
    public StructuralText getContent() {
        return content;
    }

    @Nullable
    @Override
    public ContentDetailsMetadata getMetadata() {
        return new ArticleDetailsMetadata(cover);
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public String getCover() {
        return cover;
    }

    public Article fork(Long id) {
        return new Article(id, resourceId, userId, title, cover,
                content, createTime, updateTime
        );
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getContentId() {
        return resourceId;
    }

    @NonNull
    @Override
    public ContentType getContentType() {
        return ContentType.ARTICLE;
    }

    public final static class Builder implements EntityBuilder<Article, String> {
        private Long id;
        private String resourceId;
        private long userId;
        private String title;
        private String cover;
        private StructuralText content;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;

        public Builder() {

        }

        public Builder(Article article) {
            this.id = article.id;
            this.resourceId = article.resourceId;
            this.userId = article.userId;
            this.title = article.title;
            this.cover = article.cover;
            this.content = article.content;
            this.createTime = article.createTime;
            this.updateTime = article.updateTime;
        }

        @Override
        public Builder setEntityId(String id) {
            this.resourceId = id;
            return this;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setContent(StructuralText content) {
            this.content = content;
            return this;
        }

        public Builder setCreateTime(OffsetDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder setUpdateTime(OffsetDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public Builder setCover(String cover) {
            this.cover = cover;
            return this;
        }

        @Override
        public Article build() {
            return new Article(id, resourceId, userId, title, cover, content,
                    createTime, updateTime);
        }
    }
}
