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

package tech.lamprism.lampray.storage.monitoring;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.session.UploadSessionStatus;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public class StorageStatisticsEngine {
    private static final String ACTIVE_FILE_COUNT_JPQL =
            "select count(fileEntity) from StorageFileEntity fileEntity where fileEntity.deleted = false";
    private static final String ACTIVE_LOGICAL_BYTES_JPQL =
            "select coalesce(sum(fileEntity.fileSize), 0) from StorageFileEntity fileEntity where fileEntity.deleted = false";
    private static final String BLOB_COUNT_JPQL =
            "select count(blob) from StorageBlobEntity blob";
    private static final String BLOB_UNIQUE_BYTES_JPQL =
            "select coalesce(sum(blob.fileSize), 0) from StorageBlobEntity blob";
    private static final String PLACEMENT_COUNT_JPQL =
            "select count(placement) from StorageBlobPlacementEntity placement, StorageBlobEntity blob where placement.blobId = blob.blobId";
    private static final String PLACEMENT_PHYSICAL_BYTES_JPQL =
            "select coalesce(sum(blob.fileSize), 0) from StorageBlobPlacementEntity placement, StorageBlobEntity blob where placement.blobId = blob.blobId";
    private static final String SESSION_COUNT_BY_STATUS_JPQL =
            "select count(sessionEntity) from StorageUploadSessionEntity sessionEntity where sessionEntity.status = :status";
    private static final String PENDING_SESSION_COUNT_JPQL =
            "select count(sessionEntity) from StorageUploadSessionEntity sessionEntity where sessionEntity.status = :status and sessionEntity.expiresAt >= :now";
    private static final String EXPIRED_SESSION_COUNT_JPQL =
            "select count(sessionEntity) from StorageUploadSessionEntity sessionEntity where sessionEntity.status = :expiredStatus or (sessionEntity.status = :pendingStatus and sessionEntity.expiresAt < :now)";
    private static final String BACKEND_TOTALS_JPQL =
            "select blob.primaryBackend, count(blob), coalesce(sum(blob.fileSize), 0) from StorageBlobEntity blob group by blob.primaryBackend";
    private static final String BACKEND_PLACEMENT_TOTALS_JPQL =
            "select placement.backendName, count(placement), coalesce(sum(blob.fileSize), 0) from StorageBlobPlacementEntity placement, StorageBlobEntity blob where placement.blobId = blob.blobId group by placement.backendName";
    private static final String GROUP_TOTALS_JPQL =
            "select fileEntity.groupName, count(fileEntity), coalesce(sum(fileEntity.fileSize), 0), count(distinct fileEntity.blobId) from StorageFileEntity fileEntity where fileEntity.deleted = false group by fileEntity.groupName";
    private static final String GROUP_UNIQUE_ROWS_JPQL =
            "select distinct fileEntity.groupName, blob.blobId, blob.fileSize from StorageFileEntity fileEntity, StorageBlobEntity blob where fileEntity.deleted = false and fileEntity.blobId = blob.blobId";

    private final EntityManager entityManager;

    public StorageStatisticsEngine(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public StorageOverviewTotals loadOverviewTotals(OffsetDateTime now) {
        long fileCount = scalarLong(ACTIVE_FILE_COUNT_JPQL);
        long logicalBytes = scalarLong(ACTIVE_LOGICAL_BYTES_JPQL);
        long blobCount = scalarLong(BLOB_COUNT_JPQL);
        long uniqueBytes = scalarLong(BLOB_UNIQUE_BYTES_JPQL);
        long placementCount = scalarLong(PLACEMENT_COUNT_JPQL);
        long physicalBytes = scalarLong(PLACEMENT_PHYSICAL_BYTES_JPQL);
        Map<String, Long> sessionCounts = new LinkedHashMap<>();
        sessionCounts.put(
                StorageUploadSessionState.PENDING.name(),
                scalarLong(PENDING_SESSION_COUNT_JPQL, Map.of("status", UploadSessionStatus.PENDING, "now", now))
        );
        sessionCounts.put(
                StorageUploadSessionState.COMPLETED.name(),
                scalarLong(SESSION_COUNT_BY_STATUS_JPQL, Map.of("status", UploadSessionStatus.COMPLETED))
        );
        sessionCounts.put(
                StorageUploadSessionState.EXPIRED.name(),
                scalarLong(
                        EXPIRED_SESSION_COUNT_JPQL,
                        Map.of(
                                "expiredStatus", UploadSessionStatus.EXPIRED,
                                "pendingStatus", UploadSessionStatus.PENDING,
                                "now", now
                        )
                )
        );
        return new StorageOverviewTotals(fileCount, logicalBytes, blobCount, uniqueBytes, placementCount, physicalBytes, sessionCounts);
    }

    public Map<String, StorageBackendTotals> loadBackendTotals() {
        Map<String, StorageBackendTotals> result = new LinkedHashMap<>();
        for (Object[] row : rows(BACKEND_TOTALS_JPQL)) {
            String backendName = stringValue(row[0]);
            result.put(backendName, new StorageBackendTotals(
                    longValue(row[1]),
                    longValue(row[2]),
                    0L,
                    0L
            ));
        }
        for (Object[] row : rows(BACKEND_PLACEMENT_TOTALS_JPQL)) {
            String backendName = stringValue(row[0]);
            StorageBackendTotals previous = result.getOrDefault(backendName, new StorageBackendTotals(0L, 0L, 0L, 0L));
            result.put(backendName, new StorageBackendTotals(
                    previous.getPrimaryBlobCount(),
                    previous.getUniqueBytes(),
                    longValue(row[1]),
                    longValue(row[2])
            ));
        }
        return result;
    }

    public Map<String, StorageGroupTotals> loadGroupTotals() {
        Map<String, StorageGroupTotals> result = new LinkedHashMap<>();
        for (Object[] row : rows(GROUP_TOTALS_JPQL)) {
            String groupName = stringValue(row[0]);
            result.put(groupName, new StorageGroupTotals(
                    longValue(row[1]),
                    longValue(row[2]),
                    longValue(row[3]),
                    0L
            ));
        }
        for (Object[] row : rows(GROUP_UNIQUE_ROWS_JPQL)) {
            String groupName = stringValue(row[0]);
            StorageGroupTotals previous = result.getOrDefault(groupName, new StorageGroupTotals(0L, 0L, 0L, 0L));
            result.put(groupName, new StorageGroupTotals(
                    previous.getFileCount(),
                    previous.getLogicalBytes(),
                    previous.getDistinctBlobCount(),
                    previous.getUniqueBytes() + longValue(row[2])
            ));
        }
        return result;
    }

    private long scalarLong(String jpql) {
        return scalarLong(jpql, Map.of());
    }

    private long scalarLong(String jpql,
                             Map<String, Object> parameters) {
        var query = entityManager.createQuery(jpql, Number.class);
        parameters.forEach(query::setParameter);
        Number value = query.getSingleResult();
        if (value == null) {
            return 0L;
        }
        return value.longValue();
    }

    private List<Object[]> rows(String jpql) {
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }
}
