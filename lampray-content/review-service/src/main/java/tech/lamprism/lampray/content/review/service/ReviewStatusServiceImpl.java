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
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.content.review.ReviewFeedback;
import tech.lamprism.lampray.content.review.ReviewJobInfo;
import tech.lamprism.lampray.content.review.ReviewerAllocator;
import tech.lamprism.lampray.content.review.common.ReviewErrorCode;
import tech.lamprism.lampray.content.review.common.ReviewException;
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity;
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class ReviewStatusServiceImpl implements ReviewStatusService {
    private final ReviewJobRepository reviewJobRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewerAllocator reviewerAllocator;

    public ReviewStatusServiceImpl(ReviewJobRepository reviewJobRepository,
                                   ReviewTaskRepository reviewTaskRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   ReviewerAllocator reviewerAllocator) {
        this.reviewJobRepository = reviewJobRepository;
        this.reviewTaskRepository = reviewTaskRepository;
        this.eventPublisher = eventPublisher;
        this.reviewerAllocator = reviewerAllocator;
    }

//    @Override
//    @Transactional(dontRollbackOn = CommonRuntimeException.class)
//    @Deprecated
//    public ReviewJobInfo makeReview(String jobId, long operator,
//                                    boolean passed, String reason) throws ReviewException {
//        ReviewJobEntity job = reviewJobRepository.findById(jobId).orElse(null);
//        if (job == null) {
//            throw new ReviewException(CommonErrorCode.ERROR_NOT_FOUND);
//        }
//        if (job.getStatus().isReviewed()) {
//            throw new ReviewException(ReviewErrorCode.ERROR_REVIEWED, "Already reviewed, create new review job instead.");
//        }
//        ReviewJobEntity reviewed = switchStatus(job, operator, passed, reason);
//        reviewed = reviewJobRepository.save(reviewed);
//
//        ReviewJob reviewedJob = reviewed.lock();
//        OnReviewStateChangeEvent event = new OnReviewStateChangeEvent(reviewedJob,
//                job.getStatus(), reviewed.getStatus());
//        eventPublisher.publishEvent(event);
//        return ReviewJobInfo.of(reviewedJob);
//    }

    @Override
    @Transactional(dontRollbackOn = CommonRuntimeException.class)
    public void makeReview(String jobId, String taskId,
                           long operator, ReviewFeedback feedback) throws ReviewException {
        ReviewJobEntity job = reviewJobRepository.findById(jobId).orElseThrow(
                () -> new ReviewException(CommonErrorCode.ERROR_NOT_FOUND)
        );
        if (job.getStatus().isReviewed()) {
            throw new ReviewException(ReviewErrorCode.ERROR_REVIEWED, "Already reviewed, create new review job instead.");
        }
        ReviewTaskEntity task = reviewTaskRepository.findById(taskId).orElseThrow(
                () -> new ReviewException(CommonErrorCode.ERROR_NOT_FOUND)
        );
        if (task.getReviewerId() != operator || StringUtils.equals(task.getReviewJobId(), jobId)) {
            throw new ReviewException(AuthErrorCode.ERROR_AUTH, "No permission to review this task.");
        }


        OffsetDateTime now = OffsetDateTime.now();
        task.setUpdateTime(now);
        task.setFeedback(feedback);
    }
}
