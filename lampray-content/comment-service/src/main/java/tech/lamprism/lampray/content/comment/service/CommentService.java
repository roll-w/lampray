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

package tech.lamprism.lampray.content.comment.service;

import org.springframework.stereotype.Service;
import space.lingu.NonNull;
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

    public CommentService(CommentRepository commentRepository,
                          ContentMetadataService contentMetadataService) {
        this.commentRepository = commentRepository;
        this.contentMetadataService = contentMetadataService;
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
        long parentId = checkParentId(commentDetailsMetadata.parentId(), operator);

        CommentDo comment = CommentDo
                .builder()
                .setUserId(uncreatedContent.getUserId())
                .setParentId(parentId)
                .setContent(uncreatedContent.getContent())
                .setCreateTime(timestamp)
                .setUpdateTime(timestamp)
                .setCommentOnId(commentDetailsMetadata.contentId())
                .setCommentOnType(commentDetailsMetadata.contentType())
                .setCommentStatus(CommentStatus.NONE)
                .build();

        return commentRepository.save(comment).lock();
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

    private long checkParentId(Long parentId, UserIdentity operator) {
        if (parentId == null || parentId == Comment.COMMENT_ROOT_ID) {
            return Comment.COMMENT_ROOT_ID;
        }
        if (parentId < 0) {
            throw new IllegalArgumentException("Parent id must be greater than 0");
        }
        CommentDo comment = commentRepository.findById(parentId).orElseThrow(
                () -> new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND)
        );
        ContentMetadata metadata = contentMetadataService.getMetadata(
                new SimpleContentIdentity(parentId, ContentType.COMMENT));
        if (!checkCanCommentOn(comment, metadata, operator)) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        return parentId;
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
