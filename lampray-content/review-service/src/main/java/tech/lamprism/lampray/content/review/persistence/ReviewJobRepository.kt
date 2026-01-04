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

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewStatus

/**
 * @author RollW
 */
@Repository
class ReviewJobRepository(
    private val reviewJobDao: ReviewJobDao
) : CommonRepository<ReviewJobEntity, String>(reviewJobDao) {
    fun findByContent(
        contentId: Long,
        contentType: ContentType,
    ): List<ReviewJobEntity> {
        return findAll(createContentSpecification(contentId, contentType))
    }

    private fun createContentSpecification(
        contentId: Long,
        contentType: ContentType
    ): Specification<ReviewJobEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentId), contentId),
                criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentType), contentType)
            )
        }

//    private fun createReviewerSpecification(
//        userId: Long,
//        statuses: List<ReviewStatus>,
//        reviewer: Boolean
//    ): Specification<ReviewJobEntity> =
//        Specification { root, _, criteriaBuilder ->
//            val reviewer = if (reviewer) {
//                criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewerId), userId)
//            } else {
//                criteriaBuilder.equal(root.get(ReviewJobEntity_.operatorId), userId)
//            }
//            if (statuses.isEmpty() || statuses.containsAll(ReviewStatus.entries)) {
//                // If no statuses are provided or all statuses are included,
//                // return the reviewer specification without filtering by status.
//                return@Specification reviewer
//            }
//            if (statuses.size == 1) {
//                // More efficient way than using `in` for a single value.
//                return@Specification criteriaBuilder.and(
//                    reviewer,
//                    criteriaBuilder.equal(root.get(ReviewJobEntity_.status), statuses[0])
//                )
//            }
//
//            criteriaBuilder.and(
//                reviewer,
//                root.get(ReviewJobEntity_.status).`in`(statuses)
//            )
//        }

    fun findByStatus(reviewStatus: ReviewStatus): List<ReviewJobEntity> {
        return findAll { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(ReviewJobEntity_.status), reviewStatus)
        }
    }

    fun findByStatuses(reviewStatus: List<ReviewStatus>): List<ReviewJobEntity> {
        return findAll { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                root.get(ReviewJobEntity_.status).`in`(reviewStatus)
            )
        }
    }
}