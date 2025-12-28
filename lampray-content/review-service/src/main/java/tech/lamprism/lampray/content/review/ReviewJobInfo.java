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

package tech.lamprism.lampray.content.review;

import space.lingu.NonNull;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentType;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public record ReviewJobInfo(
        String jobId,
        ReviewStatus status,
        ContentType type,
        long contentId,
        ReviewMark reviewMark,
        OffsetDateTime createTime,
        OffsetDateTime updateTime
) implements ReviewJobDetails {
    public static ReviewJobInfo of(ReviewJob job) {
        if (job == null) {
            return null;
        }
        return new ReviewJobInfo(
                job.getJobId(),
                job.getStatus(),
                job.getReviewContentType(),
                job.getReviewContentId(),
                job.getReviewMark(),
                job.getCreateTime(),
                job.getUpdateTime()
        );
    }

    @Override
    @NonNull
    public String getJobId() {
        return jobId;
    }

    @Override
    @NonNull
    public ReviewStatus getStatus() {
        return status;
    }

    @Override
    @NonNull
    public ReviewMark getReviewMark() {
        return reviewMark;
    }


    @Override
    @NonNull
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @Override
    @NonNull
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }


    @Override
    @NonNull
    public ContentIdentity getAssociatedContent() {
        return ContentIdentity.of(contentId, type);
    }
}
