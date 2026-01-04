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
import tech.lamprism.lampray.content.review.ReviewStatues;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserTrait;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobContentView;
import tech.lamprism.lampray.web.controller.review.model.ReviewJobView;
import tech.lamprism.lampray.web.controller.review.model.ReviewRequest;
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
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public ReviewManageController(ReviewJobProvider reviewJobProvider,
                                  ReviewContentProvider reviewContentProvider,
                                  ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.reviewJobProvider = reviewJobProvider;
        this.reviewContentProvider = reviewContentProvider;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @GetMapping("/reviews/{jobId}")
    public HttpResponseEntity<ReviewJobView> getReviewJob(
            @PathVariable("jobId") String jobId) {
        ReviewJobDetails reviewJobInfo = reviewJobProvider.getReviewJobDetails(jobId);
        return HttpResponseEntity.success(ReviewJobView.from(reviewJobInfo));
    }

    @GetMapping("/reviews")
    public HttpResponseEntity<List<ReviewJobView>> getReviewJobs(
            @RequestParam(value = "status", required = false,
                    defaultValue = "ALL")
            ReviewStatues statues) {
        List<ReviewJobSummary> reviewJobInfos = reviewJobProvider.getReviewJobs(statues);
        return HttpResponseEntity.success(reviewJobInfos
                .stream()
                .map(ReviewJobView::from)
                .toList()
        );
    }

    @GetMapping("/users/{userId}/reviews")
    public HttpResponseEntity<List<ReviewJobView>> getReviewJobsByUser(
            @PathVariable(value = "userId") Long userId,
            @RequestParam(value = "status", required = false,
                    defaultValue = "ALL")
            ReviewStatues statues) {
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
        ContextThread<ApiContext> apiContextThread = apiContextThreadAware.getContextThread();
        ApiContext apiContext = apiContextThread.getContext();
        UserIdentity user = Verify.verifyNotNull(apiContext.getUser());
        // TODO
        return HttpResponseEntity.success();
    }
}
