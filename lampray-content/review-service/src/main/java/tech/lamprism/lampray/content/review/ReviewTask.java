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

package tech.lamprism.lampray.content.review;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * Immutable review job task value object that implements both DataEntity and ReviewJobTaskDetails.
 *
 * @author RollW
 */
public class ReviewTask implements DataEntity<String>, ReviewTaskDetails {
    private final long id;
    private final String taskId;
    private final String reviewJobId;
    private final ReviewStatus taskStatus;
    private final long reviewerId;
    private final ReviewFeedback feedback;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;

    public ReviewTask(long id, String taskId, String reviewJobId,
                      ReviewStatus taskStatus, long reviewerId,
                      ReviewFeedback feedback,
                      OffsetDateTime createTime, OffsetDateTime updateTime) {
        this.id = id;
        this.taskId = taskId;
        this.reviewJobId = reviewJobId;
        this.taskStatus = taskStatus;
        this.reviewerId = reviewerId;
        this.feedback = feedback;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public long getId() {
        return id;
    }

    @Nullable
    @Override
    public String getEntityId() {
        return getTaskId();
    }

    @NonNull
    @Override
    public String getTaskId() {
        return taskId;
    }

    @NonNull
    @Override
    public String getReviewJobId() {
        return reviewJobId;
    }

    @NonNull
    @Override
    public ReviewStatus getStatus() {
        return taskStatus;
    }

    @NonNull
    @Override
    public long getReviewerId() {
        return reviewerId;
    }

    @Nullable
    @Override
    public ReviewFeedback getFeedback() {
        return feedback;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return ReviewTaskResourceKind.INSTANCE;
    }
}
