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

package tech.lamprism.lampray.web.controller.review;

import com.google.common.base.Verify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.content.review.ReviewContentProvider;
import tech.lamprism.lampray.content.review.ReviewJobContent;
import tech.lamprism.lampray.content.review.ReviewJobDetails;
import tech.lamprism.lampray.content.review.ReviewJobProvider;
import tech.lamprism.lampray.content.review.ReviewJobSummary;
import tech.lamprism.lampray.content.review.ReviewStatus;
import tech.lamprism.lampray.content.review.ReviewTaskCoordinator;
import tech.lamprism.lampray.content.review.ReviewTaskDetails;
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.Api;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobContentView;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobDetailsView;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobView;
import tech.lamprism.lampray.web.controller.review.model.ReviewRequest;
import tech.lamprism.lampray.web.controller.review.model.ReviewTaskView;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;

import java.util.List;

/**
 * @author RollW
 */
@Api
public class ReviewController {
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewJobProvider reviewJobProvider;
    private final ReviewContentProvider reviewContentProvider;
    private final ReviewTaskCoordinator reviewTaskCoordinator;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;


    public ReviewController(ReviewJobProvider reviewJobProvider,
                            ReviewContentProvider reviewContentProvider,
                            ReviewTaskCoordinator reviewTaskCoordinator,
                            ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.reviewJobProvider = reviewJobProvider;
        this.reviewContentProvider = reviewContentProvider;
        this.reviewTaskCoordinator = reviewTaskCoordinator;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    private UserIdentity getCurrentUser() {
        ContextThread<ApiContext> apiContextThread = apiContextThreadAware.getContextThread();
        ApiContext apiContext = apiContextThread.getContext();
        return Verify.verifyNotNull(apiContext.getUser());
    }

    @GetMapping("/reviews/{jobId}")
    public HttpResponseEntity<ReviewJobDetailsView> getReviewJobDetail(
            @PathVariable("jobId") String jobId) {
        ReviewJobDetails reviewJobDetails = reviewJobProvider.getReviewJobDetails(jobId);
        UserIdentity user = getCurrentUser();
        boolean assigned = reviewJobDetails.getTasks().stream()
                .anyMatch(task -> task.getReviewerId() == user.getOperatorId());
        if (!assigned) {
            return HttpResponseEntity.of(AuthErrorCode.ERROR_PERMISSION_DENIED);
        }

        return HttpResponseEntity.success(ReviewJobDetailsView.from(reviewJobDetails));
    }

    /**
     * Get current user's review infos.
     */
    @GetMapping({"/reviews"})
    public HttpResponseEntity<List<ReviewJobView>> getReviewInfos(
            @RequestParam(value = "statues", required = false, defaultValue = "")
            List<ReviewStatus> statues) {
        UserIdentity user = getCurrentUser();
        List<ReviewJobSummary> reviewJobInfos = reviewJobProvider
                .getReviewJobs(user, statues);
        return HttpResponseEntity.success(reviewJobInfos
                .stream()
                .map(ReviewJobView::from)
                .toList()
        );
    }

    @GetMapping("/reviews/{jobId}/content")
    public HttpResponseEntity<ReviewJobContentView> getReviewContent(
            @PathVariable("jobId") String jobId) {
        UserIdentity user = getCurrentUser();
        ReviewJobDetails jobDetails = reviewJobProvider.getReviewJobDetails(jobId);

        boolean assignedContent = jobDetails.getTasks().stream()
                .anyMatch(task -> task.getReviewerId() ==  user.getOperatorId());
        if (!assignedContent) {
            return HttpResponseEntity.of(AuthErrorCode.ERROR_PERMISSION_DENIED);
        }

        ReviewJobContent reviewJobContent = reviewContentProvider.getReviewContent(jobId);
        return HttpResponseEntity.success(
                ReviewJobContentView.of(reviewJobContent)
        );
    }

    @PostMapping("/reviews/{jobId}/tasks/{taskId}/review")
    public HttpResponseEntity<ReviewJobView> makeReview(
            @PathVariable("jobId") String jobId,
            @PathVariable("taskId") String taskId,
            @RequestBody ReviewRequest reviewRequest
    ) {
        UserIdentity user = getCurrentUser();
        ReviewFeedback feedback = reviewRequest.toFeedback();
        reviewTaskCoordinator.submitFeedback(
                jobId,
                taskId,
                user.getOperatorId(),
                feedback
        );

        ReviewJobDetails updatedJob = reviewJobProvider.getReviewJobDetails(jobId);
        return HttpResponseEntity.success(ReviewJobView.from(updatedJob));
    }

    @PostMapping("/reviews/{jobId}/tasks/{taskId}/claim")
    public HttpResponseEntity<ReviewTaskView> claimTask(
            @PathVariable("jobId") String jobId,
            @PathVariable("taskId") String taskId) {
        UserIdentity user = getCurrentUser();

        ReviewTaskDetails taskDetails = reviewTaskCoordinator.claimTask(
                jobId,
                taskId,
                user.getOperatorId()
        );

        return HttpResponseEntity.success(ReviewTaskView.from(taskDetails));
    }

    @PostMapping("/reviews/{jobId}/tasks/{taskId}/return")
    public HttpResponseEntity<ReviewTaskView> returnTask(
            @PathVariable("jobId") String jobId,
            @PathVariable("taskId") String taskId) {
        UserIdentity user = getCurrentUser();

        ReviewTaskDetails taskDetails = reviewTaskCoordinator.returnTask(
                jobId,
                taskId,
                user.getOperatorId()
        );

        return HttpResponseEntity.success(ReviewTaskView.from(taskDetails));
    }
}
