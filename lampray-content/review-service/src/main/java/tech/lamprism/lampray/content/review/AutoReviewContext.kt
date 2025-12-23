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
package tech.lamprism.lampray.content.review

import tech.lamprism.lampray.content.ContentDetails
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList


/**
 * @author RollW
 */
class AutoReviewContext(
    val reviewJob: ReviewJobDetails,
    val contentDetails: ContentDetails
) {
    private val outcomes: MutableList<Outcome> = CopyOnWriteArrayList()

    /**
     * Record an outcome for a reviewer. Thread-safe.
     */
    fun addOutcome(reviewer: AutoReviewer, verdict: Verdict, reason: String?, durationMillis: Long) {
        outcomes.add(Outcome(reviewer, verdict, reason ?: "", durationMillis))
    }

    /**
     * Convenience: mark approve from reviewer.
     */
    fun approve(reviewer: AutoReviewer) {
        addOutcome(reviewer, Verdict.APPROVE, null, 0)
    }

    /**
     * Convenience: mark reject from reviewer with reason.
     */
    fun reject(reviewer: AutoReviewer, reason: String) {
        addOutcome(reviewer, Verdict.REJECT, reason, 0)
    }

    fun getOutcomes(): List<Outcome> {
        return Collections.unmodifiableList(outcomes)
    }

    /**
     * Return first recorded outcome for the given reviewer or null if none recorded.
     */
    fun getOutcomeFrom(reviewer: AutoReviewer): Outcome? {
        return outcomes.firstOrNull { it.reviewer === reviewer }
    }

    /**
     * Whether any outcome has been recorded by the specified reviewer.
     */
    fun hasOutcomeFrom(reviewer: AutoReviewer): Boolean {
        return outcomes.any { it.reviewer === reviewer }
    }

    /**
     * Decide overall approval: if any explicit REJECT exists, treat as rejected.
     * Otherwise approved.
     */
    fun isApproved(): Boolean {
        return outcomes.none { it.verdict == Verdict.REJECT }
    }

    data class Outcome(
        val reviewer: AutoReviewer,
        val verdict: Verdict,
        val reason: String,
        val durationMillis: Long
    )

    enum class Verdict {
        APPROVE,
        REJECT,
        FAILED,
        TIMEOUT
    }
}
