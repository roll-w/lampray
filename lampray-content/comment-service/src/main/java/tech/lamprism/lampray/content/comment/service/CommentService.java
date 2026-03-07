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

package tech.lamprism.lampray.content.comment.service;

import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentDetailsMetadata;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentMetadata;
import tech.lamprism.lampray.content.ContentPublisher;
import tech.lamprism.lampray.content.ContentStatus;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.SimpleContentIdentity;
import tech.lamprism.lampray.content.UncreatedContent;
import tech.lamprism.lampray.content.collection.ContentCollectionIdentity;
import tech.lamprism.lampray.content.collection.ContentCollectionProvider;
import tech.lamprism.lampray.content.collection.ContentCollectionType;
import tech.lamprism.lampray.content.comment.Comment;
import tech.lamprism.lampray.content.comment.CommentDetailsMetadata;
import tech.lamprism.lampray.content.comment.CommentStatus;
import tech.lamprism.lampray.content.comment.persistence.CommentDo;
import tech.lamprism.lampray.content.comment.persistence.CommentRepository;
import tech.lamprism.lampray.content.common.ContentErrorCode;
import tech.lamprism.lampray.content.common.ContentException;
import tech.lamprism.lampray.content.service.ContentMetadataService;
import tech.lamprism.lampray.user.UserIdentity;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class CommentService implements ContentPublisher, ContentCollectionProvider {
    private final CommentRepository commentRepository;
    private final ContentMetadataService contentMetadataService;
    private final ResourceIdGenerator resourceIdGenerator;

    public CommentService(CommentRepository commentRepository,
                          ContentMetadataService contentMetadataService,
                          ResourceIdGenerator resourceIdGenerator) {
        this.commentRepository = commentRepository;
        this.contentMetadataService = contentMetadataService;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Override
    public ContentDetails publish(@NonNull UncreatedContent uncreatedContent,
                                  OffsetDateTime timestamp)
            throws ContentException {
        UserIdentity operator = uncreatedContent.getOperator();
        ContentDetailsMetadata detailsMetadata = uncreatedContent.getMetadata();
        if (!(detailsMetadata instanceof CommentDetailsMetadata commentDetailsMetadata)) {
            throw new IllegalArgumentException(
                    "Metadata must be an instance of CommentDetailsMetadata"
            );
        }
        ContentIdentity commentOn = ContentIdentity.of(
                commentDetailsMetadata.contentId(),
                commentDetailsMetadata.contentType()
        );
        checkCommentOnContent(commentOn, operator);
        String parentId = checkParentId(commentDetailsMetadata.parentId(), operator);

        CommentDo comment = CommentDo
                .builder()
                .setResourceId(resourceIdGenerator.nextId(ContentType.COMMENT.getSystemResourceKind()))
                .setUserId(operator.getUserId())
                .setParentId(parentId)
                .setContent(uncreatedContent.getContent())
                .setCreateTime(timestamp)
                .setUpdateTime(timestamp)
                .setCommentOnId(commentDetailsMetadata.contentId())
                .setCommentOnType(commentDetailsMetadata.contentType())
                .setCommentStatus(CommentStatus.NONE)
                .build();

        CommentDo created = commentRepository.saveAndFlush(comment);
        return created.lock();
    }

    private void checkCommentOnContent(
            ContentIdentity parentContent, UserIdentity operator) {
        ContentMetadata metadata = contentMetadataService.getMetadata(parentContent);
        if (metadata.getContentStatus() != ContentStatus.PUBLISHED) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        if (!metadata.getContentAccessAuthType().needsAuth()) {
            return;
        }
        if (operator.getUserId() == metadata.getUserId()) {
            return;
        }
        // TODO: check auth when needed
        throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
    }

    @Nullable
    private String checkParentId(@Nullable String parentId, UserIdentity operator) {
        String normalizedParentId = parentId == null ? null : parentId.trim();
        if (normalizedParentId == null || normalizedParentId.isEmpty() || "0".equals(normalizedParentId)) {
            return null;
        }

        CommentDo comment = commentRepository.findById(normalizedParentId).orElse(null);
        if (comment == null) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        ContentMetadata metadata = contentMetadataService.getMetadata(
                new SimpleContentIdentity(comment.getEntityId(), ContentType.COMMENT));
        if (!checkCanCommentOn(comment, metadata, operator)) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        return comment.getEntityId();
    }

    private boolean checkCanCommentOn(
            Content content,
            ContentMetadata contentMetadata,
            UserIdentity operator) {
        if (contentMetadata.getContentStatus() != ContentStatus.PUBLISHED) {
            return false;
        }
        if (!contentMetadata.getContentAccessAuthType().needsAuth()) {
            return true;
        }
        return operator.getUserId() == contentMetadata.getUserId();
    }

    @Override
    public boolean supports(@NonNull ContentType contentType) {
        return contentType == ContentType.COMMENT;
    }

    @NonNull
    @Override
    public List<? extends ContentDetails> getContents(
            ContentCollectionIdentity contentCollectionIdentity) {
        return getCommentsBy(contentCollectionIdentity)
                .stream()
                .map(CommentDo::lock)
                .toList();
    }

    @NonNull
    public List<CommentDo> getCommentsBy(
            ContentCollectionIdentity contentCollectionIdentity) {
        return switch (contentCollectionIdentity.getContentCollectionType()) {
            case COMMENTS -> commentRepository.findAll();
            case ARTICLE_COMMENTS -> commentRepository.findByContent(
                    contentCollectionIdentity.getContentCollectionId(),
                    ContentType.ARTICLE
            );
            default -> throw new UnsupportedOperationException("Unsupported collection type: " +
                    contentCollectionIdentity.getContentCollectionType());
        };
    }

    @Override
    public boolean supportsCollection(@NonNull ContentCollectionType contentCollectionType) {
        return switch (contentCollectionType) {
            case COMMENTS, ARTICLE_COMMENTS -> true;
            default -> false;
        };
    }
}
