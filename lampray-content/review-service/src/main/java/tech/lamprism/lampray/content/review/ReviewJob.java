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

import com.google.common.base.Preconditions;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.content.ContentAssociated;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.SimpleContentIdentity;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
public class ReviewJob implements DataEntity<Long>, ContentAssociated, ReviewJobDetails {
    private final Long jobId;

    // TODO: may add serial number to replace job id in the future
    // private String serialNumber;
    private final long reviewContentId;
    private final ContentType reviewContentType;

    // TODO: may change to reviewer ids in the future
    private final Long reviewerId;
    @Nullable
    private final Long operatorId;
    private final ReviewStatus status;
    private final String result;
    private final OffsetDateTime assignedTime;
    private final OffsetDateTime reviewTime;
    private final ReviewMark reviewMark;
    private final ContentIdentity associatedContent;

    public ReviewJob(Long jobId, long reviewContentId,
                     ContentType reviewContentType, Long reviewerId,
                     @Nullable Long operatorId,
                     ReviewStatus status,
                     String result, OffsetDateTime assignedTime,
                     OffsetDateTime reviewTime,
                     ReviewMark reviewMark) {
        Preconditions.checkNotNull(status);
        Preconditions.checkNotNull(reviewContentType);
        Preconditions.checkNotNull(reviewMark);

        this.jobId = jobId;
        this.reviewContentId = reviewContentId;
        this.reviewerId = reviewerId;
        this.operatorId = operatorId;
        this.status = status;
        this.reviewContentType = reviewContentType;
        this.assignedTime = assignedTime;
        this.result = result;
        this.reviewTime = reviewTime;
        this.reviewMark = reviewMark;
        this.associatedContent = new SimpleContentIdentity(reviewContentId, reviewContentType);
    }

    @Override
    public Long getId() {
        return getJobId();
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return getAssignedTime();
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return getReviewTime();
    }

    public long getJobId() {
        return jobId;
    }

    public long getReviewContentId() {
        return reviewContentId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    @Nullable
    public Long getOperatorId() {
        return operatorId;
    }

    @NonNull
    public ReviewStatus getStatus() {
        return status;
    }

    public ContentType getReviewContentType() {
        return reviewContentType;
    }

    @NonNull
    public OffsetDateTime getAssignedTime() {
        return assignedTime;
    }

    @NonNull
    public String getResult() {
        return result;
    }

    @NonNull
    public OffsetDateTime getReviewTime() {
        return reviewTime;
    }

    @NonNull
    public ReviewMark getReviewMark() {
        return reviewMark;
    }

    @NonNull
    @Override
    public ContentIdentity getAssociatedContent() {
        return associatedContent;
    }

    public ReviewJob reviewPass(long operatorId, OffsetDateTime reviewTime) {
        return new ReviewJob(
                jobId, reviewContentId, reviewContentType,
                reviewerId, operatorId,
                ReviewStatus.APPROVED, null, assignedTime, reviewTime,
                reviewMark);
    }

    public ReviewJob reviewReject(long operatorId, String result, OffsetDateTime reviewTime) {
        return new ReviewJob(
                jobId, reviewContentId, reviewContentType,
                reviewerId, operatorId,
                ReviewStatus.REJECTED, result, assignedTime, reviewTime,
                reviewMark);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewJob job = (ReviewJob) o;
        return Objects.equals(reviewerId, job.reviewerId) && Objects.equals(operatorId, job.operatorId) && assignedTime == job.assignedTime && reviewTime == job.reviewTime && Objects.equals(jobId, job.jobId) && Objects.equals(reviewContentId, job.reviewContentId) && status == job.status && reviewContentType == job.reviewContentType && Objects.equals(result, job.result) && reviewMark == job.reviewMark;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, reviewContentId, reviewerId, operatorId, status, reviewContentType, assignedTime, result, reviewTime, reviewMark);
    }

    @Override
    public String toString() {
        return "ReviewJob{" +
                "jobId=" + jobId +
                ", reviewContentId='" + reviewContentId + '\'' +
                ", reviewerId=" + reviewerId +
                ", operatorId=" + operatorId +
                ", status=" + status +
                ", type=" + reviewContentType +
                ", assignedTime=" + assignedTime +
                ", result='" + result + '\'' +
                ", reviewTime=" + reviewTime +
                ", reviewMark=" + reviewMark +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReviewJob fork(long id) {
        return new ReviewJob(
                id, reviewContentId, reviewContentType,
                reviewerId, operatorId, status, result, assignedTime,
                reviewTime,
                reviewMark);
    }

    @Override
    @NonNull
    public SystemResourceKind getSystemResourceKind() {
        return ReviewJobResourceKind.INSTANCE;
    }

    @Override
    public long getReviewer() {
        return reviewerId;
    }

    @Override
    @Nullable
    public Long getOperator() {
        return operatorId;
    }


    public final static class Builder implements LongEntityBuilder<ReviewJob> {
        private Long jobId = null;
        private long reviewContentId;
        private Long reviewerId;
        private Long operatorId;
        private ReviewStatus status;
        private ContentType reviewContentType;
        private OffsetDateTime assignedTime;
        private String result;
        private OffsetDateTime reviewTime;
        private ReviewMark reviewMark;

        public Builder() {
        }

        public Builder(ReviewJob job) {
            this.jobId = job.jobId;
            this.reviewContentId = job.reviewContentId;
            this.reviewerId = job.reviewerId;
            this.operatorId = job.operatorId;
            this.status = job.status;
            this.reviewContentType = job.reviewContentType;
            this.assignedTime = job.assignedTime;
            this.result = job.result;
            this.reviewTime = job.reviewTime;
            this.reviewMark = job.reviewMark;
        }

        @Override
        public LongEntityBuilder<ReviewJob> setId(Long id) {
            return setJobId(id);
        }

        public Builder setJobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder setReviewContentId(long reviewContentId) {
            this.reviewContentId = reviewContentId;
            return this;
        }

        public Builder setReviewerId(Long reviewerId) {
            this.reviewerId = reviewerId;
            return this;
        }

        public Builder setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public Builder setStatus(ReviewStatus status) {
            this.status = status;
            return this;
        }

        public Builder setReviewContentType(ContentType reviewContentType) {
            this.reviewContentType = reviewContentType;
            return this;
        }

        public Builder setAssignedTime(OffsetDateTime assignedTime) {
            this.assignedTime = assignedTime;
            return this;
        }

        public Builder setResult(String result) {
            this.result = result;
            return this;
        }

        public Builder setReviewTime(OffsetDateTime reviewTime) {
            this.reviewTime = reviewTime;
            return this;
        }

        public Builder setReviewMark(ReviewMark reviewMark) {
            this.reviewMark = reviewMark;
            return this;
        }

        @Override
        public ReviewJob build() {
            return new ReviewJob(
                    jobId, reviewContentId,
                    reviewContentType, reviewerId, operatorId, status,
                    result, assignedTime, reviewTime,
                    reviewMark);
        }

    }
}
