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

package tech.lamprism.lampray.content.review.service;

import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentTrait;
import tech.lamprism.lampray.content.ContentType;
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

/**
 * @author RollW
 */
@Service
public class ReviewServiceImpl implements ReviewService, ReviewJobProvider {
    private final ReviewJobRepository reviewJobRepository;
    private final ReviewerAllocator reviewerAllocator;

    public ReviewServiceImpl(ReviewJobRepository reviewJobRepository,
                             ReviewerAllocator reviewerAllocator) {
        this.reviewJobRepository = reviewJobRepository;
        this.reviewerAllocator = reviewerAllocator;
    }

    @Override
    public ReviewJobInfo assignReviewer(long contentId,
                                        ContentType contentType,
                                        boolean allowAutoReview) {
        OffsetDateTime assignedTime = OffsetDateTime.now();
        // TODO: may has multiple review jobs for the same content
        ReviewJobDo old = reviewJobRepository.findByContent(contentId, contentType)
                .orElse(null);
        if (old != null && !old.getStatus().isReviewed()) {
            // if the old review job is still not reviewed, throw exception
            // we don't want to assign a new reviewer to the same content
            throw new NotReviewedException(ReviewJobInfo.of(old.lock()));
        }

        long reviewerId = reviewerAllocator.allocateReviewer(
                ContentIdentity.of(contentId, contentType),
                allowAutoReview
        );

        ReviewJobDo.Builder builder = ReviewJobDo.builder()
                .setReviewContentId(contentId)
                .setReviewerId(reviewerId)
                .setReviewContentType(contentType)
                .setStatus(ReviewStatus.NOT_REVIEWED)
                .setAssignedTime(assignedTime)
                .setReviewMark(ReviewMark.NORMAL);
        if (old != null) {
            // TODO:
            builder.setReviewMark(ReviewMark.REPORT);
        }

        ReviewJobDo reviewJob = builder.build();
        reviewJob = reviewJobRepository.save(reviewJob);
        return ReviewJobInfo.of(reviewJob.lock());
    }

    @Override
    @NonNull
    public ReviewJobInfo getReviewJob(long reviewJobId) {
        ReviewJobDo reviewJob = reviewJobRepository.findById(reviewJobId)
                .orElse(null);
        if (reviewJob == null) {
            throw new ReviewException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        return ReviewJobInfo.of(reviewJob.lock());
    }

    // TODO: implement
    @Override
    @NonNull
    public List<ReviewJobInfo> getReviewJobs() {
        return List.of();
    }

    @Override
    @NonNull
    public List<ReviewJobInfo> getReviewJobs(long userId) {
        return List.of();
    }

    @Override
    @NonNull
    public List<ReviewJobInfo> getReviewJobs(ContentTrait contentTrait) {
        return List.of();
    }

    @Override
    @NonNull
    public List<ReviewJobInfo> getReviewJobs(ReviewStatus reviewStatus) {

        return List.of();
    }

    @Override
    @NonNull
    public List<ReviewJobInfo> getReviewJobs(ReviewStatues reviewStatues) {
        return List.of();
    }

    @NonNull
    @Override
    public List<ReviewJobInfo> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatus status) {
        return List.of();
    }

    @NonNull
    @Override
    public List<ReviewJobInfo> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatues statues) {
        List<ReviewJobDo> reviewJobs = reviewJobRepository.findByReviewer(
                reviewer.getOperatorId(),
                statues.getStatuses()
        );
        return reviewJobs.stream()
                .map(ReviewJobDo::lock)
                .map(ReviewJobInfo::of)
                .toList();
    }
}
