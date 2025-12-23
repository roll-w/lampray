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

import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.review.ReviewJobInfo;
import tech.lamprism.lampray.content.review.ReviewerAllocator;
import tech.lamprism.lampray.content.review.common.NotReviewedException;

/**
 * @author RollW
 */
public interface ReviewService {
    // TODO: needs rename

    default ReviewJobInfo assignReviewer(Content content)
            throws NotReviewedException {
        return assignReviewer(content, false);
    }

    /**
     * Assign a reviewer to a content.
     *
     * @param allowAutoReview if false, disable {@link ReviewerAllocator#AUTO_REVIEWER
     *                        auto reviewer} and force to assign a staff reviewer.
     */
    ReviewJobInfo assignReviewer(Content content, boolean allowAutoReview)
            throws NotReviewedException;

}
