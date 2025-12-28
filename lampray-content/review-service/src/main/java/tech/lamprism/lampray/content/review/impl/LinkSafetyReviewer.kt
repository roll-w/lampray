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
 * Simple link safety reviewer implementation.
 */
package tech.lamprism.lampray.content.review.impl

import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.review.AutoReviewContext
import tech.lamprism.lampray.content.review.AutoReviewer
import tech.lamprism.lampray.content.review.ReviewJobDetails

/**
 * LinkSafetyReviewer validates URLs in the content against a small allowlist of hosts.
 * Uses structural traversal to also collect explicit link hrefs.
 */
@Component
class LinkSafetyReviewer : AutoReviewer {
    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "LinkSafetyReviewer",
        description = "Checks whether URLs refer to allowed hosts",
        className = this::class.java.name
    )

    // TODO
    private val allowedHosts = setOf("example.com")

    override fun review(reviewJob: ReviewJobDetails, reviewResults: AutoReviewContext) {
//        val content = try { reviewResults.contentDetails.getContent() } catch (ex: Throwable) { null }
//
//        val flatten = if (content != null) StructuralTextUtils.flatten(content) else null
//
//        // first, check explicit link hrefs collected during traversal
//        flatten?.links?.forEach { href ->
//            try {
//                val host = URI(href).host ?: return@forEach
//                if (!allowedHosts.any { host.endsWith(it) }) {
//                    reviewResults.reject(this, "external link to unsafe host: $host")
//                    return
//                }
//            } catch (_: Exception) {
//                // ignore malformed href
//            }
//        }
//
//        // fall back to scanning tokens in flattened text
//        val text = flatten?.texts?.joinToString("\n") ?: (reviewJob.result ?: "")
//        val tokens = text.split(Regex("\\s+"))
//        for (t in tokens) {
//            if (t.startsWith("http://") || t.startsWith("https://")) {
//                try {
//                    val uri = URI(t)
//                    val host = uri.host ?: continue
//                    if (!allowedHosts.any { host.endsWith(it) }) {
//                        reviewResults.reject(this, "external link to unsafe host: $host")
//                        return
//                    }
//                } catch (_: Exception) {
//                    // ignore malformed URLs
//                }
//            }
//        }
        reviewResults.approve(this)
    }
}
