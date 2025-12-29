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
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class ReviewJobTask implements DataEntity<String> {
    private final long id;
    private final String taskId;
    private final String reviewJobId;
    private final ReviewStatus taskStatus;
    private final Long reviewerId;
    private final String detail;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;

    public ReviewJobTask(long id, String taskId, String reviewJobId,
                         ReviewStatus taskStatus, Long reviewerId,
                         String detail,
                         OffsetDateTime createTime, OffsetDateTime updateTime) {
        this.id = id;
        this.taskId = taskId;
        this.reviewJobId = reviewJobId;
        this.taskStatus = taskStatus;
        this.reviewerId = reviewerId;
        this.detail = detail;
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

    public String getTaskId() {
        return taskId;
    }

    public String getReviewJobId() {
        return reviewJobId;
    }

    public ReviewStatus getTaskStatus() {
        return taskStatus;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getDetail() {
        return detail;
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
        return ReviewJobTaskResourceKind.INSTANCE;
    }
}
