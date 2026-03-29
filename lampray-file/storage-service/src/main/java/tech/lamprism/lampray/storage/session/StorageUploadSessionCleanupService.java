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

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Cleans up expired and completed upload sessions.
 * @author RollW
 */
public interface StorageUploadSessionCleanupService {
    /**
     * Marks overdue pending sessions as expired.
     */
    int expireOverdueSessions(OffsetDateTime now);

    /**
     * Removes expired sessions and any orphaned uploads.
     */
    int purgeExpiredSessions(OffsetDateTime now) throws IOException;

    /**
     * Removes completed sessions past their retention window.
     */
    int purgeCompletedSessions(OffsetDateTime now);
}
