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
import tech.lamprism.lampray.user.UserTrait;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.review.model.ReassignTaskRequest;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobContentView;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobDetailsView;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobView;
import tech.lamprism.lampray.web.controller.review.model.ReviewRequest;
import tech.lamprism.lampray.web.controller.review.model.ReviewTaskView;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.system.ContextThread;
import tech.rollw.common.web.system.ContextThreadAware;

import java.util.List;

/**
 * @author RollW
 */
@AdminApi
public class ReviewManageController {
    private final ReviewJobProvider reviewJobProvider;
    private final ReviewContentProvider reviewContentProvider;
    private final ReviewTaskCoordinator reviewTaskCoordinator;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public ReviewManageController(ReviewJobProvider reviewJobProvider,
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
        ReviewJobDetails reviewJobInfo = reviewJobProvider.getReviewJobDetails(jobId);
        return HttpResponseEntity.success(ReviewJobDetailsView.from(reviewJobInfo));
    }

    @GetMapping("/reviews")
    public HttpResponseEntity<List<ReviewJobView>> getReviewJobs(
            @RequestParam(value = "statues", required = false, defaultValue = "")
            List<ReviewStatus> statues) {
        List<ReviewJobSummary> reviewJobInfos = reviewJobProvider.getReviewJobs(statues);
        return HttpResponseEntity.success(reviewJobInfos
                .stream()
                .map(ReviewJobView::from)
                .toList()
        );
    }

    @GetMapping("/users/{userId}/reviews")
    public HttpResponseEntity<List<ReviewJobView>> getReviewJobsByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "statues", required = false, defaultValue = "")
            List<ReviewStatus> statues) {
        List<ReviewJobSummary> reviewJobInfos = reviewJobProvider.getReviewJobs(
                UserTrait.of(userId),
                statues
        );
        return HttpResponseEntity.success(reviewJobInfos
                .stream()
                .map(ReviewJobView::from)
                .toList()
        );
    }

    @GetMapping("/reviews/{jobId}/content")
    public HttpResponseEntity<ReviewJobContentView> getContentOfReviewJob(
            @PathVariable("jobId") String jobId) {
        ReviewJobContent reviewJobContent = reviewContentProvider.getReviewContent(jobId);
        return HttpResponseEntity.success(ReviewJobContentView.of(reviewJobContent));
    }

    @PostMapping("/reviews/{jobId}")
    public HttpResponseEntity<ReviewJobView> makeReview(
            @PathVariable("jobId") String jobId,
            @RequestBody ReviewRequest reviewRequest
    ) {
        UserIdentity user = getCurrentUser();
        ReviewJobDetails reviewJobDetails = reviewJobProvider.getReviewJobDetails(jobId);
        List<ReviewTaskDetails> tasks = reviewJobDetails.getTasks();

        // Admin can review any task - find or create task for current user
        ReviewTaskDetails userTask = tasks.stream()
                .filter(task -> task.getReviewerId() == user.getOperatorId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No task assigned to current user"
                ));

        // Convert request to feedback using the new structure
        ReviewFeedback feedback = reviewRequest.toFeedback();

        reviewTaskCoordinator.submitFeedback(
                jobId,
                userTask.getTaskId(),
                user.getOperatorId(),
                feedback
        );

        ReviewJobDetails updatedJob = reviewJobProvider.getReviewJobDetails(jobId);
        return HttpResponseEntity.success(ReviewJobView.from(updatedJob));
    }

    @PostMapping("/reviews/{jobId}/tasks/{taskId}/reassign")
    public HttpResponseEntity<ReviewTaskView> reassignTask(
            @PathVariable("jobId") String jobId,
            @PathVariable("taskId") String taskId,
            @RequestBody ReassignTaskRequest request
    ) {
        UserIdentity user = getCurrentUser();

        ReviewTaskDetails taskDetails = reviewTaskCoordinator.reassignTask(
                jobId,
                taskId,
                user.getOperatorId(),
                request.getNewReviewerId(),
                request.getReason()
        );

        return HttpResponseEntity.success(ReviewTaskView.from(taskDetails));
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
