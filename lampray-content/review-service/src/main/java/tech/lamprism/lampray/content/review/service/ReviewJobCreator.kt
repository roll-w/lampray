/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.content.review.service

import tech.lamprism.lampray.content.Content
import tech.lamprism.lampray.content.review.ReviewJobInfo
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.common.NotReviewedException

/**
 * Creator for review jobs that handles job creation and reviewer allocation.
 *
 * @author RollW
 */
interface ReviewJobCreator {
    /**
     * Creates a review job and assigns reviewers for the given content.
     * This method will:
     * 1. Create a review job
     * 2. Allocate human reviewers (if available)
     * 3. Create review tasks for allocated reviewers
     * 4. Trigger automated review process
     *
     * @param content the content to be reviewed
     * @param reviewMark the review mark/priority
     * @return the created review job information
     * @throws NotReviewedException if a pending review job already exists for this content
     */
    fun createReviewJob(content: Content, reviewMark: ReviewMark): ReviewJobInfo
}

