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

package tech.rollw.common.value.constraint;

import space.lingu.NonNull;

/**
 * Validation result.
 *
 * @author RollW
 */
public interface ValueValidationResult {
    /**
     * Whether the validation is successful.
     *
     * @return true if the validation is successful, false otherwise
     */
    boolean isValid();

    /**
     * Get the error message if validation failed.
     *
     * @return the error message, or null if validation is successful
     */
    String getErrorMessage();

    /**
     * Create a successful validation result.
     *
     * @return the successful result
     */
    @NonNull
    static ValueValidationResult success() {
        return SimpleValueValidationResult.SUCCESS;
    }

    /**
     * Create a failed validation result with error message.
     *
     * @param errorMessage the error message
     * @return the failed result
     */
    @NonNull
    static ValueValidationResult failure(@NonNull String errorMessage) {
        return new SimpleValueValidationResult(false, errorMessage);
    }
}

