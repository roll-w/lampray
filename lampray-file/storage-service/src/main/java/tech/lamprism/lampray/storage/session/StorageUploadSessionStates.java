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

package tech.lamprism.lampray.storage.session;

import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

public final class StorageUploadSessionStates {
    public static StorageUploadSessionState resolveTrackedState(StorageUploadSessionEntity session,
                                                                OffsetDateTime now) {
        return resolveTrackedState(session.getStatus(), session.getExpiresAt(), now);
    }

    public static StorageUploadSessionState resolveTrackedState(UploadSessionStatus status,
                                                                OffsetDateTime expiresAt,
                                                                OffsetDateTime now) {
        if (status == UploadSessionStatus.PENDING && expiresAt.isBefore(now)) {
            return StorageUploadSessionState.EXPIRED;
        }
        return switch (status) {
            case PENDING -> StorageUploadSessionState.PENDING;
            case COMPLETED -> StorageUploadSessionState.COMPLETED;
            case EXPIRED -> StorageUploadSessionState.EXPIRED;
        };
    }

    private StorageUploadSessionStates() {
    }
}
