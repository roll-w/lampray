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

/**
 * Actions that can be performed on a review task.
 *
 * @author RollW
 */
enum class ReviewTaskAction {
    /**
     * Submit review feedback for the task.
     */
    SUBMIT,

    /**
     * Reassign the task to another reviewer.
     */
    REASSIGN,

    /**
     * Return the task for reassignment (reject the assignment).
     */
    RETURN_FOR_REASSIGNMENT,

    /**
     * Cancel the review task.
     */
    CANCEL,

    /**
     * Claim an unassigned task.
     */
    CLAIM
}

