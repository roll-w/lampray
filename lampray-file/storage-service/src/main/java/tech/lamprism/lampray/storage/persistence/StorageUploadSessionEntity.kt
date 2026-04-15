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

package tech.lamprism.lampray.storage.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.proxy.HibernateProxy
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageResourceKind
import tech.lamprism.lampray.storage.StorageUploadMode
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel
import tech.lamprism.lampray.storage.session.UploadSessionStatus
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "storage_upload_session")
class StorageUploadSessionEntity(
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Id
    @Column(name = "upload_id", nullable = false, length = 64, unique = true)
    var uploadId: String = "",

    @Column(name = "file_id", nullable = false, length = 64)
    var fileId: String = "",

    @Column(name = "group_name", nullable = false, length = 80)
    var groupName: String = "",

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = "",

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "mime_type", nullable = false, length = 255)
    var mimeType: String = "application/octet-stream",

    @Column(name = "file_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var fileType: FileType = FileType.OTHER,

    @Column(name = "content_checksum", length = 384)
    var contentChecksum: String? = null,

    @Column(name = "owner_user_id")
    var ownerUserId: Long? = null,

    @Column(name = "primary_backend", nullable = false, length = 80)
    var primaryBackend: String = "",

    @Column(name = "object_key", length = 512)
    var objectKey: String? = null,

    @Column(name = "upload_mode", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var uploadMode: StorageUploadMode = StorageUploadMode.PROXY,

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var status: UploadSessionStatus = UploadSessionStatus.PENDING,

    @Column(name = "expires_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var expiresAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),
) : DataEntity<String> {
    override fun getEntityId(): String = uploadId

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    override fun getSystemResourceKind(): SystemResourceKind = StorageResourceKind

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    fun toBuilder(): Builder = Builder(this)

    fun lock(): StorageUploadSessionEntity = toBuilder().build()

    class Builder {
        private var id: Long? = null
        private var uploadId: String? = null
        private var fileId: String = ""
        private var groupName: String = ""
        private var fileName: String = ""
        private var fileSize: Long? = null
        private var mimeType: String = "application/octet-stream"
        private var fileType: FileType = FileType.OTHER
        private var contentChecksum: String? = null
        private var ownerUserId: Long? = null
        private var primaryBackend: String = ""
        private var objectKey: String? = null
        private var uploadMode: StorageUploadMode = StorageUploadMode.PROXY
        private var status: UploadSessionStatus = UploadSessionStatus.PENDING
        private var expiresAt: OffsetDateTime = OffsetDateTime.now()
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        constructor(other: StorageUploadSessionEntity) {
            this.id = other.id
            this.uploadId = other.uploadId
            this.fileId = other.fileId
            this.groupName = other.groupName
            this.fileName = other.fileName
            this.fileSize = other.fileSize
            this.mimeType = other.mimeType
            this.fileType = other.fileType
            this.contentChecksum = other.contentChecksum
            this.ownerUserId = other.ownerUserId
            this.primaryBackend = other.primaryBackend
            this.objectKey = other.objectKey
            this.uploadMode = other.uploadMode
            this.status = other.status
            this.expiresAt = other.expiresAt
            this.createTime = other.createTime
            this.updateTime = other.updateTime
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setUploadId(uploadId: String) = apply {
            this.uploadId = uploadId
        }

        fun setFileId(fileId: String) = apply {
            this.fileId = fileId
        }

        fun setGroupName(groupName: String) = apply {
            this.groupName = groupName
        }

        fun setFileName(fileName: String) = apply {
            this.fileName = fileName
        }

        fun setFileSize(fileSize: Long?) = apply {
            this.fileSize = fileSize
        }

        fun setMimeType(mimeType: String) = apply {
            this.mimeType = mimeType
        }

        fun setFileType(fileType: FileType) = apply {
            this.fileType = fileType
        }

        fun setContentChecksum(contentChecksum: String?) = apply {
            this.contentChecksum = contentChecksum
        }

        fun setOwnerUserId(ownerUserId: Long?) = apply {
            this.ownerUserId = ownerUserId
        }

        fun setPrimaryBackend(primaryBackend: String) = apply {
            this.primaryBackend = primaryBackend
        }

        fun setObjectKey(objectKey: String?) = apply {
            this.objectKey = objectKey
        }

        fun setUploadMode(uploadMode: StorageUploadMode) = apply {
            this.uploadMode = uploadMode
        }

        fun setStatus(status: UploadSessionStatus) = apply {
            this.status = status
        }

        fun setExpiresAt(expiresAt: OffsetDateTime) = apply {
            this.expiresAt = expiresAt
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): StorageUploadSessionEntity {
            return StorageUploadSessionEntity(
                id = id,
                uploadId = uploadId!!,
                fileId = fileId,
                groupName = groupName,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                fileType = fileType,
                contentChecksum = contentChecksum,
                ownerUserId = ownerUserId,
                primaryBackend = primaryBackend,
                objectKey = objectKey,
                uploadMode = uploadMode,
                status = status,
                expiresAt = expiresAt,
                createTime = createTime,
                updateTime = updateTime,
            )
        }
    }

    companion object {
        @JvmStatic
        fun StorageUploadSessionEntity.toEntity(): StorageUploadSessionEntity = toBuilder().build()

        @JvmStatic
        fun StorageUploadSessionModel.toEntity(): StorageUploadSessionEntity = entity.toBuilder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val otherClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisClass = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisClass != otherClass) return false
        other as StorageUploadSessionEntity
        return uploadId == other.uploadId
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
