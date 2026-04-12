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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.proxy.HibernateProxy
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(
    name = "storage_blob",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["content_checksum"], name = "uc_storage_blob_content_checksum")
    ]
)
class StorageBlobEntity(
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Id
    @Column(name = "blob_id", nullable = false, length = 64, unique = true)
    var blobId: String = "",

    @Column(name = "content_checksum", nullable = false, length = 64)
    var contentChecksum: String = "",

    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,

    @Column(name = "mime_type", nullable = false, length = 255)
    var mimeType: String = "application/octet-stream",

    @Column(name = "file_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var fileType: FileType = FileType.OTHER,

    @Column(name = "primary_backend", nullable = false, length = 80)
    var primaryBackend: String = "",

    @Column(name = "primary_object_key", nullable = false, length = 512)
    var primaryObjectKey: String = "",

    @Column(name = "orphaned_at")
    @Temporal(TemporalType.TIMESTAMP)
    var orphanedAt: OffsetDateTime? = null,

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),
) : DataEntity<String> {
    override fun getEntityId(): String = blobId

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

    fun lock(): StorageBlobEntity = toBuilder().build()

    class Builder {
        private var id: Long? = null
        private var blobId: String? = null
        private var contentChecksum: String = ""
        private var fileSize: Long = 0
        private var mimeType: String = "application/octet-stream"
        private var fileType: FileType = FileType.OTHER
        private var primaryBackend: String = ""
        private var primaryObjectKey: String = ""
        private var orphanedAt: OffsetDateTime? = null
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        constructor(other: StorageBlobEntity) {
            this.id = other.id
            this.blobId = other.blobId
            this.contentChecksum = other.contentChecksum
            this.fileSize = other.fileSize
            this.mimeType = other.mimeType
            this.fileType = other.fileType
            this.primaryBackend = other.primaryBackend
            this.primaryObjectKey = other.primaryObjectKey
            this.orphanedAt = other.orphanedAt
            this.createTime = other.createTime
            this.updateTime = other.updateTime
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setBlobId(blobId: String) = apply {
            this.blobId = blobId
        }

        fun setContentChecksum(contentChecksum: String) = apply {
            this.contentChecksum = contentChecksum
        }

        fun setFileSize(fileSize: Long) = apply {
            this.fileSize = fileSize
        }

        fun setMimeType(mimeType: String) = apply {
            this.mimeType = mimeType
        }

        fun setFileType(fileType: FileType) = apply {
            this.fileType = fileType
        }

        fun setPrimaryBackend(primaryBackend: String) = apply {
            this.primaryBackend = primaryBackend
        }

        fun setPrimaryObjectKey(primaryObjectKey: String) = apply {
            this.primaryObjectKey = primaryObjectKey
        }

        fun setOrphanedAt(orphanedAt: OffsetDateTime?) = apply {
            this.orphanedAt = orphanedAt
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): StorageBlobEntity {
            return StorageBlobEntity(
                id = id,
                blobId = blobId!!,
                contentChecksum = contentChecksum,
                fileSize = fileSize,
                mimeType = mimeType,
                fileType = fileType,
                primaryBackend = primaryBackend,
                primaryObjectKey = primaryObjectKey,
                orphanedAt = orphanedAt,
                createTime = createTime,
                updateTime = updateTime,
            )
        }
    }

    companion object {
        @JvmStatic
        fun StorageBlobEntity.toEntity(): StorageBlobEntity = toBuilder().build()

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
        other as StorageBlobEntity
        return blobId == other.blobId
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
