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
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewStatus

/**
 * @author RollW
 */
@Repository
class ReviewJobRepository(
    private val reviewJobDao: ReviewJobDao
) : CommonRepository<ReviewJobEntity, String>(reviewJobDao) {
    override fun <S : ReviewJobEntity> save(entity: S): S {
        return reviewJobDao.saveAndFlush(entity)
    }

    override fun <S : ReviewJobEntity> saveAll(entities: Iterable<S>): List<S> {
        return reviewJobDao.saveAllAndFlush(entities)
    }

    fun findByContent(
        contentId: Long,
        contentType: ContentType,
    ): List<ReviewJobEntity> = findAll { root, _, criteriaBuilder ->
        criteriaBuilder.and(
            criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentId), contentId),
            criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentType), contentType)
        )
    }

    fun findByContentAndStatus(
        contentId: Long,
        contentType: ContentType,
        reviewStatus: ReviewStatus
    ): List<ReviewJobEntity> = findAll { root, _, criteriaBuilder ->
        criteriaBuilder.and(
            criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentId), contentId),
            criteriaBuilder.equal(root.get(ReviewJobEntity_.reviewContentType), contentType),
            criteriaBuilder.equal(root.get(ReviewJobEntity_.status), reviewStatus)
        )
    }


    fun findByStatus(reviewStatus: ReviewStatus): List<ReviewJobEntity> = findAll { root, _, criteriaBuilder ->
        criteriaBuilder.equal(root.get(ReviewJobEntity_.status), reviewStatus)
    }

    fun findByStatuses(reviewStatuses: List<ReviewStatus>): List<ReviewJobEntity> =
        findAll { root, _, criteriaBuilder ->
            if (reviewStatuses.isEmpty() || reviewStatuses.containsAll(ReviewStatus.entries)) {
                // If no statuses are provided or all statuses are included,
                // return the reviewer specification without filtering by status.
                return@findAll null
            }
            if (reviewStatuses.size == 1) {
                // More efficient way than using `in` for a single value.
                return@findAll criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(ReviewJobEntity_.status), reviewStatuses[0])
                )
            }
            criteriaBuilder.and(
                root.get(ReviewJobEntity_.status).`in`(reviewStatuses)
            )
        }

    fun findByReviewer(reviewerId: Long, statuses: List<ReviewStatus>): List<ReviewJobEntity> =
        findAll { root, query, criteriaBuilder ->
            // Build subquery: select 1 from review_job_task t where t.review_job_id = review_job.resource_id and t.reviewer_id = :reviewerId
            val subquery = query?.subquery(String::class.java) ?: return@findAll null
            val taskRoot = subquery.from(ReviewTaskEntity::class.java)
            subquery.select(taskRoot.get(ReviewTaskEntity_.reviewJobId))
            val predicates = mutableListOf(
                criteriaBuilder.equal(
                    taskRoot.get(ReviewTaskEntity_.reviewJobId),
                    root.get(ReviewJobEntity_.resourceId)
                ),
                criteriaBuilder.equal(taskRoot.get(ReviewTaskEntity_.reviewerId), reviewerId)
            )
            subquery.where(*predicates.toTypedArray())

            // If statuses provided, combine with job status predicate
            val jobPredicate = when {
                statuses.isEmpty() || statuses.containsAll(ReviewStatus.entries) -> null
                statuses.size == 1 -> criteriaBuilder.equal(root.get(ReviewJobEntity_.status), statuses[0])
                else -> root.get(ReviewJobEntity_.status).`in`(statuses)
            }

            if (jobPredicate == null) {
                return@findAll criteriaBuilder.exists(subquery)
            }
            criteriaBuilder.and(
                criteriaBuilder.exists(subquery),
                jobPredicate
            )
        }
}