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

package tech.lamprism.lampray.content.review.persistence

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewStatus
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class ReviewJobRepository(
    private val reviewJobDao: ReviewJobDao
) : CommonRepository<ReviewJobDo, Long>(reviewJobDao) {
    fun findByContent(
        contentId: Long,
        contentType: ContentType
    ): Optional<ReviewJobDo> {
        return findOne(createContentSpecification(contentId, contentType))
    }

    private fun createContentSpecification(
        contentId: Long,
        contentType: ContentType
    ): Specification<ReviewJobDo> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(ReviewJobDo_.reviewContentId), contentId),
                criteriaBuilder.equal(root.get(ReviewJobDo_.reviewContentType), contentType)
            )
        }

    fun findByReviewer(
        reviewerId: Long,
        vararg statuses: ReviewStatus
    ): List<ReviewJobDo> {
        return findAll(createReviewerSpecification(reviewerId, statuses.toList()))
    }

    private fun createReviewerSpecification(
        reviewerId: Long,
        statuses: List<ReviewStatus>
    ): Specification<ReviewJobDo> =
        Specification { root, _, criteriaBuilder ->
            val reviewer = criteriaBuilder.equal(root.get(ReviewJobDo_.reviewerId), reviewerId)
            if (statuses.isEmpty()) {
                return@Specification reviewer
            }
            criteriaBuilder.and(
                reviewer,
                root.get(ReviewJobDo_.status).`in`(statuses)
            )
        }

    fun findByStatus(reviewStatus: ReviewStatus): List<ReviewJobDo> {
        return findAll(createStatusSpecification(reviewStatus))
    }

    private fun createStatusSpecification(reviewStatus: ReviewStatus): Specification<ReviewJobDo> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(ReviewJobDo_.status), reviewStatus)
        }
}