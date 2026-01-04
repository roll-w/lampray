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
import tech.lamprism.lampray.content.ContentTrait;
import tech.lamprism.lampray.content.review.ReviewJobDetails;
import tech.lamprism.lampray.content.review.ReviewJobProvider;
import tech.lamprism.lampray.content.review.ReviewJobSummary;
import tech.lamprism.lampray.content.review.ReviewStatues;
import tech.lamprism.lampray.content.review.ReviewStatus;
import tech.lamprism.lampray.content.review.ReviewTaskDetails;
import tech.lamprism.lampray.content.review.SimpleReviewJobDetails;
import tech.lamprism.lampray.content.review.common.ReviewException;
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.system.Operator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class ReviewJobProviderImpl implements ReviewJobProvider {
    private final ReviewJobRepository reviewJobRepository;
    private final ReviewTaskRepository reviewTaskRepository;

    public ReviewJobProviderImpl(ReviewJobRepository reviewJobRepository,
                                 ReviewTaskRepository reviewTaskRepository) {
        this.reviewJobRepository = reviewJobRepository;
        this.reviewTaskRepository = reviewTaskRepository;
    }

    @NonNull
    @Override
    public ReviewJobDetails getReviewJobDetails(@NonNull String reviewJobId) {
        ReviewJobEntity reviewJob = reviewJobRepository.findById(reviewJobId)
                .orElseThrow(() -> new ReviewException(CommonErrorCode.ERROR_NOT_FOUND));
        List<ReviewTaskDetails> reviewTaskDetails = reviewTaskRepository
                .findByJobId(reviewJob.getJobId())
                .stream()
                .map(ReviewTaskEntity::lock)
                .collect(Collectors.toUnmodifiableList());
        return SimpleReviewJobDetails.toSimpleDetails(reviewJob, reviewTaskDetails);
    }

    @NonNull
    @Override
    public List<ReviewJobSummary> getReviewJobs() {
        return reviewJobRepository.findAll()
                .stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobsByOperator(@NonNull Operator operator) {
        return List.of();
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobsByReviewer(@NonNull Operator reviewer) {
        return List.of();
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobs(@NonNull ContentTrait contentTrait) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByContent(
                contentTrait.getContentId(), contentTrait.getContentType()
        );
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobs(@NonNull ReviewStatus reviewStatus) {
        List<ReviewJobEntity> reviewJobEntities = reviewJobRepository.findByStatus(reviewStatus);
        return reviewJobEntities.stream()
                .map(ReviewJobEntity::lock)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobs(@NonNull ReviewStatues reviewStatues) {
        return List.of();
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatus status) {
        return List.of();
    }

    @Override
    public @NonNull List<ReviewJobSummary> getReviewJobs(
            @NonNull Operator reviewer,
            @NonNull ReviewStatues statues) {
        return List.of();
    }
}
