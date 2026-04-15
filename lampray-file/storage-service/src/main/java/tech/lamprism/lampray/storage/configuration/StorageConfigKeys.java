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

package tech.lamprism.lampray.storage.configuration;

import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.setting.AttributedSettingSpecification;
import tech.lamprism.lampray.setting.SettingKey;
import tech.lamprism.lampray.setting.SettingSource;
import tech.lamprism.lampray.setting.SettingSpecificationBuilder;
import tech.lamprism.lampray.setting.SettingSpecificationSupplier;

import java.util.List;
import java.util.Set;

/**
 * @author RollW
 */
@Component
public class StorageConfigKeys implements SettingSpecificationSupplier {
    public static final String PREFIX = "storage.";

    public static final AttributedSettingSpecification<Set<String>, String> STORAGE_BACKENDS =
            new SettingSpecificationBuilder<>(SettingKey.ofStringSet(PREFIX + "backends"))
                    .setTextDescription("Configured storage backend names.")
                    .setRequired(false)
                    .setSupportedSources(SettingSource.LOCAL_ONLY)
                    .build();

    public static final AttributedSettingSpecification<Set<String>, String> STORAGE_GROUPS =
            new SettingSpecificationBuilder<>(SettingKey.ofStringSet(PREFIX + "groups"))
                    .setTextDescription("Configured storage group names.")
                    .setRequired(false)
                    .setSupportedSources(SettingSource.LOCAL_ONLY)
                    .build();

    public static final AttributedSettingSpecification<String, String> DEFAULT_GROUP =
            new SettingSpecificationBuilder<>(SettingKey.ofString(PREFIX + "default-group"))
                    .setTextDescription("Default storage group name used for uploads.")
                    .setDefaultValue("default")
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Boolean, Boolean> DIRECT_ACCESS_ENABLED =
            new SettingSpecificationBuilder<>(SettingKey.ofBoolean(PREFIX + "presign.enabled"))
                    .setTextDescription("Whether direct upload and download access is enabled when the backend supports it.")
                    .setDefaultValue(true)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> DIRECT_ACCESS_TTL_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "presign.ttl-seconds"))
                    .setTextDescription("Direct-access request validity duration in seconds.")
                    .setDefaultValue(300L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> UPLOAD_PROXY_THRESHOLD_BYTES =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "upload.proxy-threshold-bytes"))
                    .setTextDescription("Maximum size in bytes for proxy upload before switching to direct upload.")
                    .setDefaultValue(8L * 1024L * 1024L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> DOWNLOAD_PROXY_THRESHOLD_BYTES =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "download.proxy-threshold-bytes"))
                    .setTextDescription("Maximum size in bytes for proxy download before switching to direct download.")
                    .setDefaultValue(16L * 1024L * 1024L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> MULTIPART_THRESHOLD_BYTES =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "multipart.threshold-bytes"))
                    .setTextDescription("Reserved multipart threshold in bytes for large-object uploads.")
                    .setDefaultValue(32L * 1024L * 1024L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> MULTIPART_PART_SIZE_BYTES =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "multipart.part-size-bytes"))
                    .setTextDescription("Reserved multipart part size in bytes for large-object uploads.")
                    .setDefaultValue(16L * 1024L * 1024L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> PENDING_UPLOAD_EXPIRE_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "pending-upload-expire-seconds"))
                    .setTextDescription("Upload session expiration in seconds.")
                    .setDefaultValue(1800L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> CLEANUP_ORPHAN_UPLOAD_EXPIRE_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "cleanup.orphan-upload-expire-seconds"))
                    .setTextDescription("Maximum age in seconds before pending upload sessions are considered orphaned.")
                    .setDefaultValue(86400L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> CLEANUP_EXPIRED_UPLOAD_RETAIN_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "cleanup.expired-upload-retain-seconds"))
                    .setTextDescription("Retention period in seconds before expired upload sessions are deleted.")
                    .setDefaultValue(86400L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> CLEANUP_COMPLETED_UPLOAD_RETAIN_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "cleanup.completed-upload-retain-seconds"))
                    .setTextDescription("Retention period in seconds before completed upload sessions are deleted.")
                    .setDefaultValue(604800L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> CLEANUP_DELETED_BLOB_RETAIN_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "cleanup.deleted-blob-retain-seconds"))
                    .setTextDescription("Retention period in seconds before orphaned blobs are permanently deleted.")
                    .setDefaultValue(0L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final AttributedSettingSpecification<Long, Long> CLEANUP_INTERVAL_SECONDS =
            new SettingSpecificationBuilder<>(SettingKey.ofLong(PREFIX + "cleanup.interval-seconds"))
                    .setTextDescription("Interval in seconds between upload session cleanup runs.")
                    .setDefaultValue(600L)
                    .setRequired(true)
                    .setSupportedSources(SettingSource.VALUES)
                    .build();

    public static final StorageConfigKeys INSTANCE = new StorageConfigKeys();

    private static final List<AttributedSettingSpecification<?, ?>> SPECIFICATIONS = List.of(
            STORAGE_BACKENDS,
            STORAGE_GROUPS,
            DEFAULT_GROUP,
            DIRECT_ACCESS_ENABLED,
            DIRECT_ACCESS_TTL_SECONDS,
            UPLOAD_PROXY_THRESHOLD_BYTES,
            DOWNLOAD_PROXY_THRESHOLD_BYTES,
            MULTIPART_THRESHOLD_BYTES,
            MULTIPART_PART_SIZE_BYTES,
            PENDING_UPLOAD_EXPIRE_SECONDS,
            CLEANUP_ORPHAN_UPLOAD_EXPIRE_SECONDS,
            CLEANUP_EXPIRED_UPLOAD_RETAIN_SECONDS,
            CLEANUP_COMPLETED_UPLOAD_RETAIN_SECONDS,
            CLEANUP_DELETED_BLOB_RETAIN_SECONDS,
            CLEANUP_INTERVAL_SECONDS
    );

    @NonNull
    @Override
    public List<AttributedSettingSpecification<?, ?>> getSpecifications() {
        return SPECIFICATIONS;
    }
}
