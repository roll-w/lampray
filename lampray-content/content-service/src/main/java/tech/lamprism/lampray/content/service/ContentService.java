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

package tech.lamprism.lampray.content.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentAccessAuthType;
import tech.lamprism.lampray.content.ContentAccessCredentials;
import tech.lamprism.lampray.content.ContentAccessService;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentMetadata;
import tech.lamprism.lampray.content.ContentMetadataDetails;
import tech.lamprism.lampray.content.ContentProvider;
import tech.lamprism.lampray.content.ContentProviderFactory;
import tech.lamprism.lampray.content.ContentPublishProvider;
import tech.lamprism.lampray.content.ContentPublisher;
import tech.lamprism.lampray.content.ContentStatus;
import tech.lamprism.lampray.content.ContentSupportableUtils;
import tech.lamprism.lampray.content.ContentTrait;
import tech.lamprism.lampray.content.UncreatedContent;
import tech.lamprism.lampray.content.UncreatedContentPreChecker;
import tech.lamprism.lampray.content.collection.ContentCollectionIdentity;
import tech.lamprism.lampray.content.collection.ContentCollectionProvider;
import tech.lamprism.lampray.content.collection.ContentCollectionProviderFactory;
import tech.lamprism.lampray.content.collection.ContentCollectionType;
import tech.lamprism.lampray.content.common.ContentErrorCode;
import tech.lamprism.lampray.content.common.ContentException;
import tech.lamprism.lampray.content.permit.ContentPermitChecker;
import tech.lamprism.lampray.content.permit.ContentPermitResult;
import tech.lamprism.lampray.content.persistence.ContentMetadataDo;
import tech.lamprism.lampray.content.persistence.ContentMetadataRepository;
import tech.lamprism.lampray.content.publish.ContentPublishListener;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.system.UnsupportedKindException;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class ContentService implements ContentAccessService,
        ContentPublishProvider, ContentCollectionProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);
    private final List<ContentPublisher> contentPublishers;
    private final List<UncreatedContentPreChecker> uncreatedContentPreCheckers;
    private final List<ContentCollectionProvider> contentCollectionProviders;
    private final List<ContentPublishListener> contentPublishListeners;

    private final ContentProviderFactory contentProviderFactory;
    private final ContentPermitChecker contentPermitChecker;
    private final ContentMetadataRepository contentMetadataRepository;

    public ContentService(List<ContentPublisher> contentPublishers,
                          List<UncreatedContentPreChecker> uncreatedContentPreCheckers,
                          List<ContentCollectionProvider> contentCollectionProviders,
                          List<ContentPublishListener> contentPublishListeners,
                          ContentProviderFactory contentProviderFactory,
                          ContentPermitChecker contentPermitChecker,
                          ContentMetadataRepository contentMetadataRepository) {
        this.contentPublishers = contentPublishers;
        this.uncreatedContentPreCheckers = uncreatedContentPreCheckers;
        this.contentCollectionProviders = contentCollectionProviders;
        this.contentPublishListeners = contentPublishListeners;
        this.contentProviderFactory = contentProviderFactory;
        this.contentPermitChecker = contentPermitChecker;
        this.contentMetadataRepository = contentMetadataRepository;
    }

    /**
     * @inheritDoc
     */
    @Override
    public ContentDetails openContent(ContentTrait contentTrait,
                                      ContentAccessCredentials contentAccessCredentials) throws ContentException {
        ContentMetadataDo metadata = contentMetadataRepository
                .findByContent(contentTrait)
                .orElseThrow(() -> new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND));
        ErrorCode errorCode = fromContentStatus(metadata.getContentStatus());
        if (errorCode.failed()) {
            throw new ContentException(errorCode);
        }
        ContentPermitResult permitResult = contentPermitChecker.checkAccessPermit(
                Content.of(metadata.getUserId(), contentTrait),
                metadata.getContentAccessAuthType(),
                contentAccessCredentials
        );
        if (!permitResult.isPermitted()) {
            throw new ContentException(
                    selectFirst(permitResult.getErrors())
            );
        }
        ContentProvider provider = contentProviderFactory.getContentProvider(
                contentTrait.getContentType());
        return provider.getContentDetails(contentTrait);
    }

    /**
     * @inheritDoc
     */
    @Override
    public ContentDetails getContentDetails(ContentTrait contentTrait)
            throws ContentException {
        return getContentMetadataDetails(contentTrait);
    }

    @Override
    public ContentMetadataDetails<?> getContentMetadataDetails(ContentTrait contentTrait)
            throws ContentException {
        ContentMetadataDo metadata = contentMetadataRepository
                .findByContent(contentTrait)
                .orElse(null);
        if (metadata == null) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        ContentProvider provider = contentProviderFactory.getContentProvider(
                contentTrait.getContentType());
        ContentDetails contentDetails = provider
                .getContentDetails(contentTrait);
        return new ContentMetadataDetails<>(contentDetails, metadata.lock());
    }

    /**
     * @inheritDoc
     */
    public ContentStatus getContentStatus(ContentTrait contentTrait)
            throws ContentException {
        ContentMetadataDo metadata = contentMetadataRepository
                .findByContent(contentTrait)
                .orElse(null);
        if (metadata == null) {
            throw new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND);
        }
        return metadata.getContentStatus();
    }

    private ErrorCode fromContentStatus(ContentStatus status) {
        return switch (status) {
            case PUBLISHED -> CommonErrorCode.SUCCESS;
            case DRAFT -> CommonErrorCode.ERROR_NOT_FOUND;
            case REVIEWING -> ContentErrorCode.ERROR_CONTENT_REVIEWING;
            case HIDE -> ContentErrorCode.ERROR_CONTENT_HIDE;
            case FORBIDDEN, REVIEW_REJECTED -> ContentErrorCode.ERROR_CONTENT_FORBIDDEN;
            case DELETED -> ContentErrorCode.ERROR_CONTENT_DELETED;
        };
    }

    private static ErrorCode selectFirst(Collection<ErrorCode> errorCodes) {
        return errorCodes.iterator().next();
    }

    @Override
    public ContentDetails publishContent(UncreatedContent uncreatedContent) throws ContentException {
        OffsetDateTime timestamp = OffsetDateTime.now();
        ContentSupportableUtils.findAllSupportable(uncreatedContentPreCheckers,
                        uncreatedContent.getContentType())
                .forEach(checker ->
                        checker.checkUncreatedContent(uncreatedContent)
                );
        ContentPublisher contentPublisher = ContentSupportableUtils.findSupportableNonNull(
                contentPublishers,
                uncreatedContent.getContentType());
        ContentDetails contentDetails = contentPublisher.publish(
                uncreatedContent,
                timestamp
        );
        ContentMetadataDo.Builder contentMetadataBuilder = ContentMetadataDo
                .builder()
                .setContentId(contentDetails.getContentId())
                .setContentType(contentDetails.getContentType())
                .setUserId(contentDetails.getUserId())
                .setContentAccessAuthType(ContentAccessAuthType.PUBLIC);
        ContentStatus contentStatus = ContentStatus.PUBLISHED;
        for (ContentPublishListener contentPublishListener :
                contentPublishListeners) {
            contentStatus = contentStatus.plus(
                    contentPublishListener.onPublish(contentDetails)
            );
        }
        contentMetadataBuilder.setContentStatus(contentStatus);
        contentMetadataRepository.save(contentMetadataBuilder.build());
        return contentDetails;
    }

    private List<ContentMetadataDetails<? extends ContentDetails>> pairWith(
            List<? extends ContentDetails> contentDetails,
            List<ContentMetadata> contentMetadata) {
        return contentDetails.stream().map(details -> {
            ContentMetadata metadata = contentMetadata.stream()
                    .filter(m -> m.getContentId() == details.getContentId())
                    .findFirst()
                    .orElseThrow(() -> new ContentException(ContentErrorCode.ERROR_CONTENT_NOT_FOUND));
            return new ContentMetadataDetails<>(details, metadata);
        }).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public ContentCollectionProvider getContentCollectionProvider(
            ContentCollectionType contentCollectionType) {
        return contentCollectionProviders.stream()
                .filter(contentCollectionProvider -> contentCollectionProvider
                        .supportsCollection(contentCollectionType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedKindException(
                        "Unsupported content collection type: " + contentCollectionType));
    }

    @Override
    public List<ContentMetadataDetails<? extends ContentDetails>> getContents(
            ContentCollectionIdentity contentCollectionIdentity,
            ContentAccessCredentials contentAccessCredentials) {
        // TODO: impl
        return getContents(contentCollectionIdentity);
    }

    @Override
    public List<ContentMetadataDetails<? extends ContentDetails>> getContents(
            ContentCollectionIdentity contentCollectionIdentity) {
        ContentCollectionProvider contentCollectionProvider = getContentCollectionProvider(
                contentCollectionIdentity.getContentCollectionType());
        List<? extends ContentDetails> contents = contentCollectionProvider
                .getContents(contentCollectionIdentity);
        List<ContentMetadata> contentMetadatas = contentMetadataRepository
                .findByContents(contents)
                .stream()
                .map(ContentMetadataDo::lock)
                .toList();
        return pairWith(contents, contentMetadatas);
    }
}
