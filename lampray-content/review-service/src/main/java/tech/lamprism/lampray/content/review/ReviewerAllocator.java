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

import tech.lamprism.lampray.content.ContentIdentity;

/**
 * @author RollW
 */
public interface ReviewerAllocator {
    /**
     * The reviewer id for auto-reviewer.
     * <p>
     * Reviewed by keyword filter or other machine learning algorithms.
     */
    long AUTO_REVIEWER = -1;

    /**
     * Allocate a reviewer for the content with the given content.
     *
     * @param allowAutoReviewer whether to allow auto-reviewer
     */
    long allocateReviewer(ContentIdentity contentIdentity, boolean allowAutoReviewer);

    /**
     * Callback when the reviewer is released.
     *
     * @param reviewerId  the reviewer id
     * @param contentIdentity the content identity
     */
    void releaseReviewer(long reviewerId, ContentIdentity contentIdentity);
}
