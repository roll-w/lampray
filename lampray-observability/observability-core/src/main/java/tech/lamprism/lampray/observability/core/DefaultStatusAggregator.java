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
        HealthStatus selected = HealthStatus.UNKNOWN;
        int selectedRank = rank(selected);
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

        return hasStatus ? selected : HealthStatus.UNKNOWN;
    }

    private int rank(HealthStatus status) {
        String code = status.getCode();
        if (HealthStatus.DOWN.getCode().equals(code)) {
            return 0;
        }
        if (HealthStatus.OUT_OF_SERVICE.getCode().equals(code)) {
            return 1;
        }
        if (HealthStatus.UNKNOWN.getCode().equals(code)) {
            return 2;
        }
        if (HealthStatus.UP.getCode().equals(code)) {
            return 3;
        }
        return 2;
    }
}
