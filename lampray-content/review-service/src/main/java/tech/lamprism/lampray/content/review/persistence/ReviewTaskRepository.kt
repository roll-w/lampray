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

package tech.lamprism.lampray.content.review.persistence

import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository

/**
 * @author RollW
 */
@Repository
class ReviewTaskRepository(
    private val reviewTaskDao: ReviewTaskDao
) : CommonRepository<ReviewTaskEntity, String>(reviewTaskDao) {

    fun findByJobId(jobId: String): List<ReviewTaskEntity> {
        return findAll { root, _, builder ->
            builder.equal(root.get(ReviewTaskEntity_.reviewJobId), jobId)
        }
    }

    fun findByReviewerId(reviewerId: Long): List<ReviewTaskEntity> {
        return findAll { root, _, builder ->
            builder.equal(root.get(ReviewTaskEntity_.reviewerId), reviewerId)
        }
    }

    fun findByJobIdAndReviewerId(jobId: String, reviewerId: Long): List<ReviewTaskEntity> {
        return findAll { root, _, builder ->
            builder.and(
                builder.equal(root.get(ReviewTaskEntity_.reviewJobId), jobId),
                builder.equal(root.get(ReviewTaskEntity_.reviewerId), reviewerId)
            )
        }
    }

}