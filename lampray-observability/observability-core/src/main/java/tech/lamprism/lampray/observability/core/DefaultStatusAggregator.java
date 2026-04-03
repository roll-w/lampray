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

package tech.lamprism.lampray.observability.core;

import tech.lamprism.lampray.observability.HealthStatus;
import tech.lamprism.lampray.observability.StatusAggregator;

/**
 * @author RollW
 */
public final class DefaultStatusAggregator implements StatusAggregator {
    @Override
    public HealthStatus aggregate(Iterable<HealthStatus> statuses) {
        HealthStatus selected = null;
        int selectedRank = Integer.MAX_VALUE;
        boolean hasStatus = false;

        for (HealthStatus status : statuses) {
            if (status == null) {
                continue;
            }
            hasStatus = true;
            int candidateRank = rank(status);
            if (candidateRank < selectedRank) {
                selected = status;
                selectedRank = candidateRank;
            }
        }

        if (!hasStatus) {
            return HealthStatus.UNKNOWN;
        }
        return selected != null ? selected : HealthStatus.UNKNOWN;
    }

    private int rank(HealthStatus status) {
        return switch (status) {
            case DOWN -> 0;
            case OUT_OF_SERVICE -> 1;
            case UP -> 3;
            default -> 2;
        };
    }
}
