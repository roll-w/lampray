/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.content.comment;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.EntityBuilder;
import tech.lamprism.lampray.content.ContentAssociated;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentDetailsMetadata;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.structuraltext.StructuralText;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class Comment implements DataEntity<String>, ContentDetails, ContentAssociated {
    public static final String COMMENT_ROOT_ID = "";

    private final Long id;
    private final String resourceId;
    private final long userId;
    /**
     * Parent comment id.
     * <p>
     * If the comment is a top-level comment,
     * the parent id is an empty string.
     */
    private final String parentId;
    private final StructuralText content;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;

    /**
     * Comment on which type of content.
     */
    private final ContentType commentOnType;
    private final String commentOnId;

    @NonNull
    private final CommentStatus commentStatus;

    private final ContentIdentity associatedContent;
    private final CommentDetailsMetadata commentDetailsMetadata;

    public Comment(Long id, String resourceId, long userId, String parentId, StructuralText content,
                   OffsetDateTime createTime, OffsetDateTime updateTime,
                   ContentType commentOnType, String commentOnId,
                   @NonNull CommentStatus commentStatus) {
        this.id = id;
        this.resourceId = resourceId;
        this.userId = userId;
        this.commentOnId = commentOnId;
        this.parentId = parentId;
        this.content = content;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.commentOnType = commentOnType;
        this.commentStatus = commentStatus;
        this.associatedContent = ContentIdentity.of(commentOnId, commentOnType);
        this.commentDetailsMetadata = new CommentDetailsMetadata(commentOnType, commentOnId, parentId);
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
    public String getContentId() {
        return resourceId;
    }

    @NonNull
    @Override
    public ContentType getContentType() {
        return ContentType.COMMENT;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public StructuralText getContent() {
        return content;
    }

    public String getParentId() {
        return parentId;
    }

    public String getCommentOnId() {
        return commentOnId;
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

    public ContentType getCommentOnType() {
        return commentOnType;
    }

    @NonNull
    public CommentStatus getCommentStatus() {
        return commentStatus;
    }

    @Override
    public ContentIdentity getAssociatedContent() {
        return associatedContent;
    }

    @Nullable
    @Override
    public ContentDetailsMetadata getMetadata() {
        return commentDetailsMetadata;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return CommentResourceKind.INSTANCE;
    }

    public static class Builder implements EntityBuilder<Comment, String> {
        private Long id;
        private String resourceId;
        private long userId;
        private String commentOn;
        private String parentId = COMMENT_ROOT_ID;
        private StructuralText content;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private ContentType type;
        @NonNull
        private CommentStatus commentStatus;

        public Builder(Comment comment) {
            this.id = comment.id;
            this.resourceId = comment.resourceId;
            this.userId = comment.userId;
            this.commentOn = comment.commentOnId;
            this.parentId = comment.parentId;
            this.content = comment.content;
            this.createTime = comment.createTime;
            this.updateTime = comment.updateTime;
            this.type = comment.commentOnType;
            this.commentStatus = comment.commentStatus;
        }

        public Builder() {
            commentStatus = CommentStatus.NONE;
        }

        @Override
        public Builder setEntityId(String id) {
            return setResourceId(id);
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setCommentOn(String commentOn) {
            this.commentOn = commentOn;
            return this;
        }

        public Builder setParentId(String parentId) {
            this.parentId = parentId;
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

        public Builder setType(ContentType type) {
            this.type = type;
            return this;
        }

        public Builder setCommentStatus(@NonNull CommentStatus commentStatus) {
            this.commentStatus = commentStatus;
            return this;
        }

        @Override
        public Comment build() {
            return new Comment(
                    id, resourceId, userId, parentId,
                    content, createTime,
                    updateTime, type,
                    commentOn, commentStatus
            );
        }
    }

}
