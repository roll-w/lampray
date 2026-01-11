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

import jakarta.transaction.Transactional
import org.slf4j.debug
import org.slf4j.info
import org.slf4j.logger
import org.slf4j.warn
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import tech.lamprism.lampray.common.data.ResourceIdGenerator
import tech.lamprism.lampray.content.Content
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentIdentity
import tech.lamprism.lampray.content.ContentProviderFactory
import tech.lamprism.lampray.content.review.ReviewJobCreator
import tech.lamprism.lampray.content.review.ReviewJobResourceKind
import tech.lamprism.lampray.content.review.ReviewJobSummary
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.ReviewTaskCoordinator
import tech.lamprism.lampray.content.review.ReviewerAllocator
import tech.lamprism.lampray.content.review.autoreview.AutoReviewOrchestrator
import tech.lamprism.lampray.content.review.common.NotReviewedException
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository
import tech.rollw.common.web.CommonRuntimeException
import java.time.OffsetDateTime

private val logger = logger<ReviewJobCreatorImpl>()

/**
 * @author RollW
 */
@Component
class ReviewJobCreatorImpl(
    private val reviewJobRepository: ReviewJobRepository,
    private val resourceIdGenerator: ResourceIdGenerator,
    private val contentProviderFactory: ContentProviderFactory,
    private val reviewerAllocator: ReviewerAllocator,
    private val reviewTaskCoordinator: ReviewTaskCoordinator,
    private val autoReviewOrchestrator: AutoReviewOrchestrator,
    private val applicationEventPublisher: ApplicationEventPublisher
) : ReviewJobCreator {

    override fun createReviewJob(content: Content, reviewMark: ReviewMark): ReviewJobSummary {
        val contentId = content.contentId
        val contentType = content.contentType

        val existingJobs = reviewJobRepository.findByContentAndStatus(contentId, contentType, ReviewStatus.PENDING)
        if (existingJobs.isNotEmpty()) {
            throw NotReviewedException(existingJobs[0].lock())
        }

        val assignedTime = OffsetDateTime.now()
        val reviewJob = ReviewJobEntity.builder()
            .setResourceId(resourceIdGenerator.nextId(ReviewJobResourceKind))
            .setReviewContentId(contentId)
            .setReviewContentType(contentType)
            .setStatus(ReviewStatus.PENDING)
            .setCreateTime(assignedTime)
            .setReviewMark(reviewMark)
            .build()

        val savedJob = reviewJobRepository.save(reviewJob)
        val reviewJobInfo = savedJob.lock()

        logger.info {
            "Created review job ${reviewJobInfo.jobId}, for content $contentId@$contentType"
        }

        // Allocate reviewers and create tasks
        allocateReviewersAndCreateTasks(reviewJobInfo, content)

        applicationEventPublisher.publishEvent(AfterCommitAutoReviewHook(reviewJobInfo, content))

        return reviewJobInfo
    }

    @Async
    @Transactional(dontRollbackOn = [CommonRuntimeException::class], rollbackOn = [Exception::class])
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun executeAutoReview(hook: AfterCommitAutoReviewHook) {
        val contentDetails = retrieveContentDetails(hook.content)
        autoReviewOrchestrator.executeAutoReview(hook.reviewJob, contentDetails)
    }

    private fun allocateReviewersAndCreateTasks(
        reviewJobInfo: ReviewJobSummary,
        content: Content
    ) {
        val contentIdentity = ContentIdentity.of(content.contentId, content.contentType)

        val humanReviewers = mutableListOf<Long>()
        try {
            val reviewerId = reviewerAllocator.allocateReviewer(contentIdentity, false)
            if (reviewerId != ReviewerAllocator.AUTO_REVIEWER) {
                humanReviewers.add(reviewerId)
            }
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to allocate human reviewer for job ${reviewJobInfo.jobId}: ${e.message}"
            }
        }

        if (humanReviewers.isNotEmpty()) {
            val tasks = reviewTaskCoordinator.createTasksForReviewers(
                reviewJobInfo.jobId,
                humanReviewers
            )
            logger.debug {
                "Created ${tasks.size} human review tasks for job ${reviewJobInfo.jobId}"
            }
        } else {
            logger.warn {
                "No human reviewers allocated for job ${reviewJobInfo.jobId}, auto-review will be primary"
            }
        }
    }

    private fun retrieveContentDetails(content: Content): ContentDetails {
        if (content is ContentDetails) {
            return content
        }

        return contentProviderFactory.getContentProvider(content.contentType)
            .getContentDetails(content)
    }
}

