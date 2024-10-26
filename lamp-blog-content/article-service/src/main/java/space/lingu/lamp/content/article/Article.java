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

package space.lingu.lamp.content.article;

import space.lingu.NonNull;
import space.lingu.Nullable;
import space.lingu.lamp.DataEntity;
import space.lingu.lamp.LongEntityBuilder;
import space.lingu.lamp.content.ContentDetails;
import space.lingu.lamp.content.ContentDetailsMetadata;
import space.lingu.lamp.content.ContentType;

import java.time.LocalDateTime;

/**
 * @author RollW
 */
public class Article implements DataEntity<Long>, ContentDetails {
    private final Long id;
    private final long userId;
    private final String title;
    private final String cover;
    private final String content;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    public Article(Long id, long userId, String title, String cover,
                   String content, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.cover = cover;
        this.content = content;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    @Override
    public Long getId() {
        return id;
    }

    @NonNull
    @Override
    public Long getResourceId() {
        return getId();
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
    public String getContent() {
        return content;
    }

    @Nullable
    @Override
    public ContentDetailsMetadata getMetadata() {
        return new ArticleDetailsMetadata(cover);
    }

    @NonNull
    @Override
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    @Override
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getCover() {
        return cover;
    }

    public Article fork(Long id) {
        return new Article(id, userId, title, cover,
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
    public long getContentId() {
        return getId();
    }

    @NonNull
    @Override
    public ContentType getContentType() {
        return ContentType.ARTICLE;
    }

    public final static class Builder implements LongEntityBuilder<Article> {
        private Long id;
        private long userId;
        private String title;
        private String cover;
        private String content;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public Builder() {

        }

        public Builder(Article article) {
            this.id = article.id;
            this.userId = article.userId;
            this.title = article.title;
            this.cover = article.cover;
            this.content = article.content;
            this.createTime = article.createTime;
            this.updateTime = article.updateTime;
        }

        @Override
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

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public Builder setCover(String cover) {
            this.cover = cover;
            return this;
        }

        @Override
        public Article build() {
            return new Article(id, userId, title, cover, content,
                    createTime, updateTime);
        }
    }
}
