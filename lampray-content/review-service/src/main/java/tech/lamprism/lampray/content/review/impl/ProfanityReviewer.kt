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

/*
 * Simple profanity reviewer implementation.
 */
package tech.lamprism.lampray.content.review.impl

import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.review.AutoReviewContext
import tech.lamprism.lampray.content.review.AutoReviewer
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.util.StructuralTextUtils

/**
 * ProfanityReviewer scans title and content for simple forbidden words using structural traversal.
 */
@Component
class ProfanityReviewer : AutoReviewer {
    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "ProfanityReviewer",
        description = "Detects forbidden words using a small deny-list",
        className = this::class.java.name
    )

    // TODO
    private val denyList = listOf("test")

    override fun review(reviewJob: ReviewJobDetails, reviewResults: AutoReviewContext) {
        val content = try { reviewResults.contentDetails.getContent() } catch (ex: Throwable) { null }
        val flattened = if (content != null) StructuralTextUtils.flatten(content).texts.joinToString("\n") else (reviewJob.result ?: "")

        for (word in denyList) {
            if (flattened.contains(word, ignoreCase = true)) {
                reviewResults.reject(this, "contains forbidden word: $word")
                return
            }
        }
        reviewResults.approve(this)
    }
}
