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

package tech.lamprism.lampray.content.review.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentMetadata;
import tech.lamprism.lampray.content.ContentStatus;
import tech.lamprism.lampray.content.SimpleContentInfo;
import tech.lamprism.lampray.content.event.ContentStatusEvent;
import tech.lamprism.lampray.content.review.ReviewJob;
import tech.lamprism.lampray.content.review.ReviewStatus;
import tech.lamprism.lampray.content.review.event.OnReviewStateChangeEvent;
import tech.lamprism.lampray.content.service.ContentMetadataService;

/**
 * Converts {@link OnReviewStateChangeEvent} to {@link ContentStatusEvent}.
 *
 * @author RollW
 */
@Component
public class OnReviewStateChangeListener implements ApplicationListener<OnReviewStateChangeEvent> {
    private final ApplicationEventPublisher eventPublisher;
    private final ContentMetadataService contentMetadataService;

    private static final Logger logger = LoggerFactory.getLogger(OnReviewStateChangeListener.class);

    public OnReviewStateChangeListener(ApplicationEventPublisher eventPublisher, ContentMetadataService contentMetadataService) {
        this.eventPublisher = eventPublisher;
        this.contentMetadataService = contentMetadataService;
    }

    @Override
    @Async
    public void onApplicationEvent(@NonNull OnReviewStateChangeEvent event) {
        ReviewJob reviewJob = event.getReviewJob();
        // TODO: replace with ContentStatusProcessor
        ContentMetadata metadata = contentMetadataService.getMetadata(
                reviewJob.getAssociatedContent()
        );
        Content content = new SimpleContentInfo(
                metadata.getUserId(),
                metadata.getContentId(),
                metadata.getContentType()
        );
        ContentStatus currentStatus = toContentStatus(event.getCurrentStatus());
        ContentStatus previousStatus = toContentStatus(event.getPreviousStatus());
        contentMetadataService.updateMetadata(
                metadata.toBuilder()
                        .setContentStatus(currentStatus)
                        .build()
        );
        ContentStatusEvent<?> contentStatusEvent = new ContentStatusEvent<>(
                content, reviewJob.getUpdateTime(),
                previousStatus, currentStatus);
        eventPublisher.publishEvent(contentStatusEvent);
    }

    private static ContentStatus toContentStatus(ReviewStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> ContentStatus.REVIEWING;
            case APPROVED -> ContentStatus.PUBLISHED;
            case REJECTED -> ContentStatus.REVIEW_REJECTED;
            case CANCELED -> ContentStatus.HIDE;
        };
    }
}
