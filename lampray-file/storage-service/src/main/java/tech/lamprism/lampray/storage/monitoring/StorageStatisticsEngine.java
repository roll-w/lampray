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

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private static final String BACKEND_NAME = "backend_name";
    private static final String PRIMARY_BLOB_COUNT = "primary_blob_count";
    private static final String UNIQUE_BYTES = "unique_bytes";
    private static final String PLACEMENT_COUNT = "placement_count";
    private static final String PHYSICAL_BYTES = "physical_bytes";
    private static final String GROUP_NAME = "group_name";
    private static final String FILE_COUNT = "file_count";
    private static final String LOGICAL_BYTES = "logical_bytes";
    private static final String DISTINCT_BLOB_COUNT = "distinct_blob_count";

    private static final String ACTIVE_FILE_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_file WHERE deleted = false";
    private static final String ACTIVE_LOGICAL_BYTES_SQL =
            "SELECT COALESCE(SUM(file_size), 0) FROM storage_file WHERE deleted = false";
    private static final String BLOB_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_blob";
    private static final String BLOB_UNIQUE_BYTES_SQL =
            "SELECT COALESCE(SUM(file_size), 0) FROM storage_blob";
    private static final String PLACEMENT_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_blob_placement p JOIN storage_blob b ON p.blob_id = b.blob_id";
    private static final String PLACEMENT_PHYSICAL_BYTES_SQL =
            "SELECT COALESCE(SUM(b.file_size), 0) FROM storage_blob_placement p JOIN storage_blob b ON p.blob_id = b.blob_id";
    private static final String PENDING_SESSION_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'PENDING' AND expires_at >= :now";
    private static final String COMPLETED_SESSION_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'COMPLETED'";
    private static final String EXPIRED_SESSION_COUNT_SQL =
            "SELECT COUNT(*) FROM storage_upload_session WHERE status = 'EXPIRED' OR (status = 'PENDING' AND expires_at < :now)";
    private static final String BACKEND_TOTALS_SQL =
            "SELECT primary_backend AS backend_name, COUNT(*) AS primary_blob_count, COALESCE(SUM(file_size), 0) AS unique_bytes " +
                    "FROM storage_blob GROUP BY primary_backend";
    private static final String BACKEND_PLACEMENT_TOTALS_SQL =
            "SELECT p.backend_name AS backend_name, COUNT(*) AS placement_count, COALESCE(SUM(b.file_size), 0) AS physical_bytes " +
                    "FROM storage_blob_placement p JOIN storage_blob b ON p.blob_id = b.blob_id GROUP BY p.backend_name";
    private static final String GROUP_TOTALS_SQL =
            "SELECT group_name AS group_name, COUNT(*) AS file_count, COALESCE(SUM(file_size), 0) AS logical_bytes, " +
                    "COUNT(DISTINCT blob_id) AS distinct_blob_count FROM storage_file WHERE deleted = false GROUP BY group_name";
    private static final String GROUP_UNIQUE_BYTES_SQL =
            "SELECT grouped.group_name AS group_name, COALESCE(SUM(b.file_size), 0) AS unique_bytes FROM storage_blob b " +
                    "JOIN (SELECT DISTINCT group_name, blob_id FROM storage_file WHERE deleted = false) grouped ON b.blob_id = grouped.blob_id " +
                    "GROUP BY grouped.group_name";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StorageStatisticsEngine(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StorageOverviewTotals loadOverviewTotals(OffsetDateTime now) {
        long fileCount = scalarLong(ACTIVE_FILE_COUNT_SQL);
        long logicalBytes = scalarLong(ACTIVE_LOGICAL_BYTES_SQL);
        long blobCount = scalarLong(BLOB_COUNT_SQL);
        long uniqueBytes = scalarLong(BLOB_UNIQUE_BYTES_SQL);
        long placementCount = scalarLong(PLACEMENT_COUNT_SQL);
        long physicalBytes = scalarLong(PLACEMENT_PHYSICAL_BYTES_SQL);
        Map<String, Object> parameters = Map.of("now", now);
        Map<String, Long> sessionCounts = new LinkedHashMap<>();
        sessionCounts.put(StorageUploadSessionState.PENDING.name(), scalarLong(PENDING_SESSION_COUNT_SQL, parameters));
        sessionCounts.put(StorageUploadSessionState.COMPLETED.name(), scalarLong(COMPLETED_SESSION_COUNT_SQL));
        sessionCounts.put(StorageUploadSessionState.EXPIRED.name(), scalarLong(EXPIRED_SESSION_COUNT_SQL, parameters));
        return new StorageOverviewTotals(fileCount, logicalBytes, blobCount, uniqueBytes, placementCount, physicalBytes, sessionCounts);
    }

    public Map<String, StorageBackendTotals> loadBackendTotals() {
        Map<String, StorageBackendTotals> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows(BACKEND_TOTALS_SQL)) {
            String backendName = stringValue(row.get(BACKEND_NAME));
            result.put(backendName, new StorageBackendTotals(
                    longValue(row.get(PRIMARY_BLOB_COUNT)),
                    longValue(row.get(UNIQUE_BYTES)),
                    0L,
                    0L
            ));
        }
        for (Map<String, Object> row : rows(BACKEND_PLACEMENT_TOTALS_SQL)) {
            String backendName = stringValue(row.get(BACKEND_NAME));
            StorageBackendTotals previous = result.getOrDefault(backendName, new StorageBackendTotals(0L, 0L, 0L, 0L));
            result.put(backendName, new StorageBackendTotals(
                    previous.getPrimaryBlobCount(),
                    previous.getUniqueBytes(),
                    longValue(row.get(PLACEMENT_COUNT)),
                    longValue(row.get(PHYSICAL_BYTES))
            ));
        }
        return result;
    }

    public Map<String, StorageGroupTotals> loadGroupTotals() {
        Map<String, StorageGroupTotals> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows(GROUP_TOTALS_SQL)) {
            String groupName = stringValue(row.get(GROUP_NAME));
            result.put(groupName, new StorageGroupTotals(
                    longValue(row.get(FILE_COUNT)),
                    longValue(row.get(LOGICAL_BYTES)),
                    longValue(row.get(DISTINCT_BLOB_COUNT)),
                    0L
            ));
        }
        for (Map<String, Object> row : rows(GROUP_UNIQUE_BYTES_SQL)) {
            String groupName = stringValue(row.get(GROUP_NAME));
            StorageGroupTotals previous = result.getOrDefault(groupName, new StorageGroupTotals(0L, 0L, 0L, 0L));
            result.put(groupName, new StorageGroupTotals(
                    previous.getFileCount(),
                    previous.getLogicalBytes(),
                    previous.getDistinctBlobCount(),
                    longValue(row.get(UNIQUE_BYTES))
            ));
        }
        return result;
    }

    private long scalarLong(String sql) {
        return scalarLong(sql, Map.of());
    }

    private long scalarLong(String sql,
                            Map<String, Object> parameters) {
        Number value = jdbcTemplate.queryForObject(sql, parameters, Number.class);
        if (value == null) {
            return 0L;
        }
        return value.longValue();
    }

    private List<Map<String, Object>> rows(String sql) {
        return jdbcTemplate.queryForList(sql, Map.of());
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
