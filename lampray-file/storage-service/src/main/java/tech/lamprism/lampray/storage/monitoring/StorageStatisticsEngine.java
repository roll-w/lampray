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
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageUploadSessionState;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public class StorageStatisticsEngine {
    @PersistenceContext
    private EntityManager entityManager;

    public StorageOverviewTotals loadOverviewTotals(OffsetDateTime now) {
        long fileCount = scalarLong("SELECT COUNT(*) FROM storage_file");
        long logicalBytes = scalarLong("SELECT COALESCE(SUM(file_size), 0) FROM storage_file");
        long blobCount = scalarLong("SELECT COUNT(*) FROM storage_blob");
        long uniqueBytes = scalarLong("SELECT COALESCE(SUM(file_size), 0) FROM storage_blob");
        long placementCount = scalarLong("SELECT COUNT(*) FROM storage_blob_placement");
        long physicalBytes = scalarLong(
                "SELECT COALESCE(SUM(b.file_size), 0) FROM storage_blob_placement p " +
                        "LEFT JOIN storage_blob b ON p.blob_id = b.blob_id"
        );
        Map<String, Long> sessionCounts = new LinkedHashMap<>();
        sessionCounts.put(StorageUploadSessionState.PENDING.name(), scalarLong(
                "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'PENDING' AND expires_at >= :now",
                Map.of("now", now)
        ));
        sessionCounts.put(StorageUploadSessionState.COMPLETED.name(), scalarLong(
                "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'COMPLETED'"
        ));
        sessionCounts.put(StorageUploadSessionState.EXPIRED.name(), scalarLong(
                "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'EXPIRED' OR (status = 'PENDING' AND expires_at < :now)",
                Map.of("now", now)
        ));
        return new StorageOverviewTotals(fileCount, logicalBytes, blobCount, uniqueBytes, placementCount, physicalBytes, sessionCounts);
    }

    public Map<String, StorageBackendTotals> loadBackendTotals() {
        Map<String, StorageBackendTotals> result = new LinkedHashMap<>();
        for (Object[] row : rows(
                "SELECT primary_backend, COUNT(*), COALESCE(SUM(file_size), 0) FROM storage_blob GROUP BY primary_backend"
        )) {
            String backendName = stringValue(row[0]);
            result.put(backendName, new StorageBackendTotals(longValue(row[1]), longValue(row[2]), 0L, 0L));
        }
        for (Object[] row : rows(
                "SELECT p.backend_name, COUNT(*), COALESCE(SUM(b.file_size), 0) FROM storage_blob_placement p " +
                        "LEFT JOIN storage_blob b ON p.blob_id = b.blob_id GROUP BY p.backend_name"
        )) {
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
        for (Object[] row : rows(
                "SELECT group_name, COUNT(*), COALESCE(SUM(file_size), 0), COUNT(DISTINCT blob_id) FROM storage_file GROUP BY group_name"
        )) {
            String groupName = stringValue(row[0]);
            result.put(groupName, new StorageGroupTotals(
                    longValue(row[1]),
                    longValue(row[2]),
                    longValue(row[3]),
                    0L
            ));
        }
        for (Object[] row : rows(
                "SELECT grouped.group_name, COALESCE(SUM(b.file_size), 0) FROM storage_blob b " +
                        "JOIN (SELECT DISTINCT group_name, blob_id FROM storage_file) grouped ON b.blob_id = grouped.blob_id " +
                        "GROUP BY grouped.group_name"
        )) {
            String groupName = stringValue(row[0]);
            StorageGroupTotals previous = result.getOrDefault(groupName, new StorageGroupTotals(0L, 0L, 0L, 0L));
            result.put(groupName, new StorageGroupTotals(
                    previous.getFileCount(),
                    previous.getLogicalBytes(),
                    previous.getDistinctBlobCount(),
                    longValue(row[1])
            ));
        }
        return result;
    }

    private long scalarLong(String sql) {
        return scalarLong(sql, Map.of());
    }

    private long scalarLong(String sql,
                            Map<String, Object> parameters) {
        Number number = (Number) createQuery(sql, parameters).getSingleResult();
        return number == null ? 0L : number.longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        return createQuery(sql, Map.of()).getResultList();
    }

    private jakarta.persistence.Query createQuery(String sql,
                                                  Map<String, Object> parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private long longValue(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
