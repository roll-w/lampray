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

import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentProviderFactory;
import tech.lamprism.lampray.content.ContentTrait;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.review.autoreview.AutoReviewService;
import tech.lamprism.lampray.content.review.ReviewJobDetails;
import tech.lamprism.lampray.content.review.ReviewJobInfo;
import tech.lamprism.lampray.content.review.ReviewJobProvider;
import tech.lamprism.lampray.content.review.ReviewMark;
import tech.lamprism.lampray.content.review.ReviewStatues;
import tech.lamprism.lampray.content.review.ReviewStatus;
import tech.lamprism.lampray.content.review.ReviewerAllocator;
import tech.lamprism.lampray.content.review.common.NotReviewedException;
import tech.lamprism.lampray.content.review.common.ReviewException;
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.system.Operator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class ReviewServiceImpl implements ReviewService, ReviewJobProvider {
    private final ReviewJobRepository reviewJobRepository;
    private final ContentProviderFactory contentProviderFactory;
    private final ReviewerAllocator reviewerAllocator;
    private final AutoReviewService autoReviewService;

    public ReviewServiceImpl(ReviewJobRepository reviewJobRepository,
                             ContentProviderFactory contentProviderFactory,
                             ReviewerAllocator reviewerAllocator,
                             AutoReviewService autoReviewService) {
        this.reviewJobRepository = reviewJobRepository;
        this.contentProviderFactory = contentProviderFactory;
        this.reviewerAllocator = reviewerAllocator;
        this.autoReviewService = autoReviewService;
    }

    @Override
    @Transactional(dontRollbackOn = CommonRuntimeException.class)
    public ReviewJobInfo assignReviewer(Content content,
                                        ReviewMark reviewMark) {
        OffsetDateTime assignedTime = OffsetDateTime.now();
        long contentId = content.getContentId();
        ContentType contentType = content.getContentType();
        List<ReviewJobEntity> old = reviewJobRepository.findByContent(contentId, contentType);
        if (!old.isEmpty()) {
            ReviewJobEntity notReviewedJob = old.stream()
                    .filter(job -> job.getStatus() == ReviewStatus.PENDING)
                    .findFirst()
                    .orElse(null);
            if (notReviewedJob != null) {
                // If the old review job is still not reviewed, throw an exception.
                // We don't want to assign a new reviewer to the same content
                throw new NotReviewedException(ReviewJobInfo.of(notReviewedJob.lock()));
            }
        }
        ReviewJobEntity.Builder builder = ReviewJobEntity.builder()
                .setReviewContentId(contentId)
                .setReviewContentType(contentType)
                .setStatus(ReviewStatus.PENDING)
                .setCreateTime(assignedTime)
                .setReviewMark(reviewMark);
        ReviewJobEntity reviewJob = builder.build();
        reviewJob = reviewJobRepository.save(reviewJob);
        long reviewerId = reviewerAllocator.allocateReviewer(
                ContentIdentity.of(contentId, contentType),
                true
        );
        ReviewJobInfo reviewJobInfo = ReviewJobInfo.of(reviewJob.lock());
        try {
            return reviewJobInfo;
        } finally {
            dispatchAutoReviewJob(reviewJobInfo, content);
        }
    }

    private ContentDetails retrieveContentDetails(ContentIdentity contentIdentity) {
        if (contentIdentity == null) {
            throw new IllegalArgumentException("Content identity cannot be null");
        }
        if (contentIdentity instanceof ContentDetails contentDetails) {
            return contentDetails;
        }

        return contentProviderFactory.getContentProvider(contentIdentity.getContentType())
                .getContentDetails(contentIdentity);
    }

    private void dispatchAutoReviewJob(ReviewJobInfo reviewJobInfo, Content content) {
        ContentDetails contentDetails = retrieveContentDetails(content);
        autoReviewService.joinAutoReviewQueue(reviewJobInfo, contentDetails);
    }

    @Override
    @NonNull
    public ReviewJobInfo getReviewJob(@NotNull String reviewJobId) {
        ReviewJobEntity reviewJob = reviewJobRepository.findById(reviewJobId)
                .orElseThrow(() -> new ReviewException(CommonErrorCode.ERROR_NOT_FOUND));
        return ReviewJobInfo.of(reviewJob.lock());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs() {
        return reviewJobRepository.findAll()
                .stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobsByOperator(@NonNull Operator operator) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByOperator(operator.getOperatorId(), List.of());
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobsByReviewer(@NonNull Operator reviewer) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByReviewer(reviewer.getOperatorId(), List.of());
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ContentTrait contentTrait) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByContent(
                contentTrait.getContentId(), contentTrait.getContentType()
        );
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ReviewStatus reviewStatus) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByStatus(reviewStatus);
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ReviewStatues reviewStatues) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByStatuses(reviewStatues.getStatuses());
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @NonNull
    @Override
    public List<ReviewJobDetails> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatus status) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository
                .findByReviewer(reviewer.getOperatorId(), List.of(status));
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @NonNull
    @Override
    public List<ReviewJobDetails> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatues statues) {
        List<ReviewJobEntity> reviewJobs = reviewJobRepository.findByReviewer(
                reviewer.getOperatorId(),
                statues.getStatuses()
        );
        return reviewJobs.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }
}
