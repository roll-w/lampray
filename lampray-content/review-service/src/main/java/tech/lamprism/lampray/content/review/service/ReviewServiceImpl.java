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

import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentProviderFactory;
import tech.lamprism.lampray.content.ContentTrait;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.review.AutoReviewService;
import tech.lamprism.lampray.content.review.ReviewJobDetails;
import tech.lamprism.lampray.content.review.ReviewJobInfo;
import tech.lamprism.lampray.content.review.ReviewJobProvider;
import tech.lamprism.lampray.content.review.ReviewMark;
import tech.lamprism.lampray.content.review.ReviewStatues;
import tech.lamprism.lampray.content.review.ReviewStatus;
import tech.lamprism.lampray.content.review.ReviewerAllocator;
import tech.lamprism.lampray.content.review.common.NotReviewedException;
import tech.lamprism.lampray.content.review.common.ReviewException;
import tech.lamprism.lampray.content.review.persistence.ReviewJobDo;
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository;
import tech.rollw.common.web.CommonErrorCode;
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
    public ReviewJobInfo assignReviewer(Content content,
                                        boolean allowAutoReview) {
        OffsetDateTime assignedTime = OffsetDateTime.now();
        long contentId = content.getContentId();
        ContentType contentType = content.getContentType();
        List<ReviewJobDo> old = reviewJobRepository.findByContent(contentId, contentType);
        if (!old.isEmpty()) {
            ReviewJobDo notReviewedJob = old.stream()
                    .filter(job -> job.getStatus() == ReviewStatus.PENDING)
                    .findFirst()
                    .orElse(null);
            if (notReviewedJob != null) {
                // if the old review job is still not reviewed, throw exception
                // we don't want to assign a new reviewer to the same content
                throw new NotReviewedException(ReviewJobInfo.of(notReviewedJob.lock()));
            }
        }

        long reviewerId = reviewerAllocator.allocateReviewer(
                ContentIdentity.of(contentId, contentType),
                allowAutoReview
        );

        ReviewJobDo.Builder builder = ReviewJobDo.builder()
                .setReviewContentId(contentId)
                .setReviewerId(reviewerId)
                .setReviewContentType(contentType)
                .setStatus(ReviewStatus.PENDING)
                .setAssignedTime(assignedTime)
                .setReviewMark(ReviewMark.NORMAL);
        if (!old.isEmpty()) {
            // TODO: could be content has been modified and needs to be re-reviewed
            builder.setReviewMark(ReviewMark.REPORT);
        }

        ReviewJobDo reviewJob = builder.build();
        reviewJob = reviewJobRepository.save(reviewJob);
        ReviewJobInfo reviewJobInfo = ReviewJobInfo.of(reviewJob.lock());
        try {
            return reviewJobInfo;
        } finally {
            if (reviewerId == ReviewerAllocator.AUTO_REVIEWER) {
                dispatchAutoReviewJob(reviewJobInfo, content, allowAutoReview);
            }
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

    private void dispatchAutoReviewJob(ReviewJobInfo reviewJobInfo, Content content, boolean autoReview) {
        if (!autoReview) {
            return;
        }
        ContentDetails contentDetails = retrieveContentDetails(content);
        autoReviewService.joinAutoReviewQueue(reviewJobInfo, contentDetails);
    }

    @Override
    @NonNull
    public ReviewJobInfo getReviewJob(long reviewJobId) {
        ReviewJobDo reviewJob = reviewJobRepository.findById(reviewJobId)
                .orElseThrow(() -> new ReviewException(CommonErrorCode.ERROR_NOT_FOUND));
        return ReviewJobInfo.of(reviewJob.lock());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs() {
        return reviewJobRepository.findAll()
                .stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobsByOperator(@NonNull Operator operator) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository.findByOperator(operator.getOperatorId(), List.of());
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobsByReviewer(@NonNull Operator reviewer) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository.findByReviewer(reviewer.getOperatorId(), List.of());
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ContentTrait contentTrait) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository.findByContent(
                contentTrait.getContentId(), contentTrait.getContentType()
        );
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ReviewStatus reviewStatus) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository.findByStatus(reviewStatus);
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @NonNull
    public List<ReviewJobDetails> getReviewJobs(@NonNull ReviewStatues reviewStatues) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository.findByStatuses(reviewStatues.getStatuses());
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @NonNull
    @Override
    public List<ReviewJobDetails> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatus status) {
        List<ReviewJobDo> reviewJobDos = reviewJobRepository
                .findByReviewer(reviewer.getOperatorId(), List.of(status));
        return reviewJobDos.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @NonNull
    @Override
    public List<ReviewJobDetails> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatues statues) {
        List<ReviewJobDo> reviewJobs = reviewJobRepository.findByReviewer(
                reviewer.getOperatorId(),
                statues.getStatuses()
        );
        return reviewJobs.stream()
                .map(ReviewJobDo::lock)
                .collect(Collectors.toUnmodifiableList());
    }
}
