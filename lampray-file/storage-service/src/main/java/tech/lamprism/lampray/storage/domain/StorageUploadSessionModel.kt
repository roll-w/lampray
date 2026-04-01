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

package tech.lamprism.lampray.storage.domain

import org.apache.commons.lang3.StringUtils
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageAccessRequest
import tech.lamprism.lampray.storage.StorageException
import tech.lamprism.lampray.storage.StorageUploadMode
import tech.lamprism.lampray.storage.StorageUploadRequest
import tech.lamprism.lampray.storage.StorageUploadSessionState
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.materialization.TempUpload
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity
import tech.lamprism.lampray.storage.session.UploadSessionStatus
import tech.lamprism.lampray.storage.store.BlobObject
import tech.rollw.common.web.AuthErrorCode
import tech.rollw.common.web.CommonErrorCode
import java.time.OffsetDateTime
import java.util.Locale

/**
 * @author RollW
 */
class StorageUploadSessionModel private constructor(
    val entity: StorageUploadSessionEntity,
    val directRequest: StorageAccessRequest? = null,
) {
    val uploadId: String
        get() = entity.uploadId
    val fileId: String
        get() = entity.fileId
    val groupName: String
        get() = entity.groupName
    val fileName: String
        get() = entity.fileName
    val fileSize: Long?
        get() = entity.fileSize
    val mimeType: String
        get() = entity.mimeType
    val fileType: FileType
        get() = entity.fileType
    val checksumSha256: String?
        get() = entity.checksumSha256
    val ownerUserId: Long?
        get() = entity.ownerUserId
    val primaryBackend: String
        get() = entity.primaryBackend
    val objectKey: String?
        get() = entity.objectKey
    val uploadMode: StorageUploadMode
        get() = entity.uploadMode
    val status: UploadSessionStatus
        get() = entity.status
    val expiresAt: OffsetDateTime
        get() = entity.expiresAt
    val createTime: OffsetDateTime
        get() = entity.createTime
    val updateTime: OffsetDateTime
        get() = entity.updateTime

    fun trackedStateAt(now: OffsetDateTime): StorageUploadSessionState {
        if (entity.status == UploadSessionStatus.PENDING && entity.expiresAt.isBefore(now)) {
            return StorageUploadSessionState.EXPIRED
        }
        return when (entity.status) {
            UploadSessionStatus.PENDING -> StorageUploadSessionState.PENDING
            UploadSessionStatus.COMPLETED -> StorageUploadSessionState.COMPLETED
            UploadSessionStatus.EXPIRED -> StorageUploadSessionState.EXPIRED
        }
    }

    fun ensureAuthorized(userId: Long?) {
        if (entity.ownerUserId != null && entity.ownerUserId != userId) {
            throw StorageException(
                AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                "You are not allowed to use this upload session.",
            )
        }
    }

    fun ensureQueryable(userId: Long?) {
        if (userId == null || entity.ownerUserId == null || entity.ownerUserId != userId) {
            throw StorageException(
                AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                "You are not allowed to query this upload session.",
            )
        }
    }

    fun expire(now: OffsetDateTime) {
        entity.status = UploadSessionStatus.EXPIRED
        entity.setUpdateTime(now)
    }

    fun markCompleted(now: OffsetDateTime) {
        entity.status = UploadSessionStatus.COMPLETED
        entity.setUpdateTime(now)
    }

    fun requireChecksum(): String =
        normalizeChecksum(entity.checksumSha256)
            ?: throw StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Direct uploads require a checksum.")

    fun validateUploadedContent(tempUpload: TempUpload, groupSettings: StorageGroupConfig) {
        validateGroupSizeLimit(groupSettings.maxSizeBytes, tempUpload.size)
        validateDeclaredSize(entity.fileSize, tempUpload.size)
        val expectedChecksum = normalizeChecksum(entity.checksumSha256)
        if (expectedChecksum != null) {
            validateChecksumMatch(expectedChecksum, tempUpload.checksumSha256)
        }
    }

    fun validateUploadedObject(uploadedObject: BlobObject, groupSettings: StorageGroupConfig) {
        validateGroupSizeLimit(groupSettings.maxSizeBytes, uploadedObject.size)
        validateDeclaredSize(entity.fileSize, uploadedObject.size)
    }

    fun requiresOrphanCleanup(hasStoredFile: Boolean): Boolean {
        if (entity.uploadMode != StorageUploadMode.DIRECT) {
            return false
        }
        if (entity.primaryBackend.isBlank() || entity.objectKey.isNullOrBlank()) {
            return false
        }
        return !hasStoredFile
    }

    fun isReadyToPurgeExpired(
        expiredRetentionCutoff: OffsetDateTime,
        orphanCleanupCutoff: OffsetDateTime,
        hasStoredFile: Boolean,
    ): Boolean {
        if (entity.updateTime.isAfter(expiredRetentionCutoff)) {
            return false
        }
        if (!requiresOrphanCleanup(hasStoredFile)) {
            return true
        }
        return !entity.expiresAt.isAfter(orphanCleanupCutoff)
    }

    companion object {
        @JvmStatic
        fun from(
            entity: StorageUploadSessionEntity,
            directRequest: StorageAccessRequest? = null,
        ): StorageUploadSessionModel = StorageUploadSessionModel(entity, directRequest)

        @JvmStatic
        fun pending(
            uploadId: String,
            fileId: String,
            groupName: String,
            fileName: String,
            fileSize: Long?,
            mimeType: String,
            fileType: FileType,
            checksumSha256: String?,
            ownerUserId: Long?,
            primaryBackend: String,
            objectKey: String?,
            uploadMode: StorageUploadMode,
            expiresAt: OffsetDateTime,
            now: OffsetDateTime,
            directRequest: StorageAccessRequest? = null,
        ): StorageUploadSessionModel =
            StorageUploadSessionModel(
                StorageUploadSessionEntity(
                    uploadId = uploadId,
                    fileId = fileId,
                    groupName = groupName,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    fileType = fileType,
                    checksumSha256 = checksumSha256,
                    ownerUserId = ownerUserId,
                    primaryBackend = primaryBackend,
                    objectKey = objectKey,
                    uploadMode = uploadMode,
                    status = UploadSessionStatus.PENDING,
                    expiresAt = expiresAt,
                    createTime = now,
                    updateTime = now,
                ),
                directRequest,
            )

        @JvmStatic
        fun normalizeFileName(fileName: String): String {
            if (StringUtils.isBlank(fileName)) {
                throw StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is required.")
            }
            var normalized = fileName.trim()
            val slashIndex = normalized.lastIndexOf('/')
            val backslashIndex = normalized.lastIndexOf('\\')
            val separatorIndex = maxOf(slashIndex, backslashIndex)
            if (separatorIndex >= 0 && separatorIndex < normalized.length - 1) {
                normalized = normalized.substring(separatorIndex + 1)
            }
            if (normalized.isBlank() || normalized == "." || normalized == "..") {
                throw StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.")
            }
            for (current in normalized) {
                if (current == '\r' || current == '\n') {
                    throw StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.")
                }
            }
            return normalized
        }

        @JvmStatic
        fun normalizeChecksum(checksumSha256: String?): String? {
            if (StringUtils.isBlank(checksumSha256)) {
                return null
            }
            val normalized = checksumSha256!!.trim().lowercase(Locale.ROOT)
            if (normalized.length != 64) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Checksum must be a 64-character SHA-256 hex string.",
                )
            }
            for (current in normalized) {
                val numeric = current in '0'..'9'
                val alphabetic = current in 'a'..'f'
                if (!numeric && !alphabetic) {
                    throw StorageException(
                        CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                        "Checksum must be a lowercase SHA-256 hex string.",
                    )
                }
            }
            return normalized
        }

        @JvmStatic
        fun validateChecksumMatch(expectedChecksum: String, actualChecksum: String) {
            if (expectedChecksum != actualChecksum) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file checksum does not match declared checksum.",
                )
            }
        }

        @JvmStatic
        fun validateUploadRequest(
            request: StorageUploadRequest,
            groupSettings: StorageGroupConfig,
            fileType: FileType,
        ) {
            val requestedSize = request.size
            if (requestedSize != null && requestedSize < 0) {
                throw StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File size cannot be negative.")
            }
            if (requestedSize != null && groupSettings.maxSizeBytes != null && requestedSize > groupSettings.maxSizeBytes!!) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.",
                )
            }
            if (groupSettings.allowedFileTypes.isNotEmpty() && !groupSettings.allowedFileTypes.contains(fileType)) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "File type is not allowed for this storage group.",
                )
            }
        }

        private fun validateGroupSizeLimit(maxSizeBytes: Long?, actualSize: Long) {
            if (maxSizeBytes != null && actualSize > maxSizeBytes) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.",
                )
            }
        }

        private fun validateDeclaredSize(declaredSize: Long?, actualSize: Long) {
            if (declaredSize != null && declaredSize != actualSize) {
                throw StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size does not match declared size.",
                )
            }
        }
    }
}
