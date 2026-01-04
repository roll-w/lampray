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
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.EntityBuilder;
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
public class ReviewJob implements DataEntity<String>, ContentAssociated, ReviewJobSummary {
    private final Long id;
    private final String jobId;
    private final long reviewContentId;
    private final ContentType reviewContentType;

    private final ReviewStatus status;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    private final ReviewMark reviewMark;
    private final ContentIdentity associatedContent;

    public ReviewJob(Long id, String jobId, long reviewContentId,
                     ContentType reviewContentType,
                     ReviewStatus status, OffsetDateTime createTime,
                     OffsetDateTime updateTime,
                     ReviewMark reviewMark) {
        Preconditions.checkNotNull(status);
        Preconditions.checkNotNull(reviewContentType);
        Preconditions.checkNotNull(reviewMark);

        this.id = id;
        this.jobId = jobId;
        this.reviewContentId = reviewContentId;
        this.status = status;
        this.reviewContentType = reviewContentType;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.reviewMark = reviewMark;
        this.associatedContent = new SimpleContentIdentity(reviewContentId, reviewContentType);
    }

    @Override
    public String getEntityId() {
        return getJobId();
    }

    public long getId() {
        return id;
    }

    public long getReviewContentId() {
        return reviewContentId;
    }

    @NonNull
    public ReviewStatus getStatus() {
        return status;
    }

    public ContentType getReviewContentType() {
        return reviewContentType;
    }

    @NonNull
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    public OffsetDateTime getUpdateTime() {
        return updateTime;
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

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewJob job = (ReviewJob) o;
        return Objects.equals(jobId, job.jobId)  && createTime == job.createTime && updateTime == job.updateTime && Objects.equals(id, job.id) && Objects.equals(reviewContentId, job.reviewContentId) && status == job.status && reviewContentType == job.reviewContentType && reviewMark == job.reviewMark;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobId, reviewContentId, status, reviewContentType, createTime, updateTime, reviewMark);
    }

    @Override
    public String toString() {
        return "ReviewJob{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", reviewContentId='" + reviewContentId + '\'' +
                ", status=" + status +
                ", type=" + reviewContentType +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", reviewMark=" + reviewMark +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    @NonNull
    public SystemResourceKind getSystemResourceKind() {
        return ReviewJobResourceKind.INSTANCE;
    }

    @Override
    @NonNull
    public String getJobId() {
        return jobId;
    }

    public final static class Builder implements EntityBuilder<ReviewJob, String> {
        private Long id = null;
        private String jobId;
        private long reviewContentId;
        private ReviewStatus status;
        private ContentType reviewContentType;
        private OffsetDateTime assignedTime;
        private OffsetDateTime reviewTime;
        private ReviewMark reviewMark;

        public Builder() {
        }

        public Builder(ReviewJob job) {
            this.id = job.id;
            this.reviewContentId = job.reviewContentId;
            this.status = job.status;
            this.reviewContentType = job.reviewContentType;
            this.assignedTime = job.createTime;
            this.reviewTime = job.updateTime;
            this.reviewMark = job.reviewMark;
        }

        @Override
        public Builder setEntityId(String id) {
            return setJobId(id);
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setJobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder setReviewContentId(long reviewContentId) {
            this.reviewContentId = reviewContentId;
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
                    id, jobId,
                    reviewContentId, reviewContentType,
                    status, assignedTime,
                    reviewTime, reviewMark);
        }

    }
}
