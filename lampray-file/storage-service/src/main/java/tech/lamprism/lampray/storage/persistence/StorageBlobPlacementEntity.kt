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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import org.hibernate.proxy.HibernateProxy
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.storage.StorageResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(
    name = "storage_blob_placement",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["blob_id", "backend_name"], name = "uc_storage_blob_backend")
    ]
)
class StorageBlobPlacementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null,

    @Column(name = "blob_id", nullable = false, length = 64)
    var blobId: String = "",

    @Column(name = "backend_name", nullable = false, length = 80)
    var backendName: String = "",

    @Column(name = "object_key", nullable = false, length = 512)
    var objectKey: String = "",

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @Column(name = "purged_at")
    @Temporal(TemporalType.TIMESTAMP)
    var purgedAt: OffsetDateTime? = null,
) : DataEntity<Long> {
    override fun getEntityId(): Long? = id

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    override fun getSystemResourceKind(): SystemResourceKind = StorageResourceKind

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    fun isDeleted(): Boolean = deleted

    fun isPurged(): Boolean = purgedAt != null

    fun toBuilder(): Builder = Builder(this)

    fun lock(): StorageBlobPlacementEntity = toBuilder().build()

    class Builder {
        private var id: Long? = null
        private var blobId: String = ""
        private var backendName: String = ""
        private var objectKey: String = ""
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()
        private var deleted: Boolean = false
        private var purgedAt: OffsetDateTime? = null

        constructor()

        constructor(other: StorageBlobPlacementEntity) {
            this.id = other.id
            this.blobId = other.blobId
            this.backendName = other.backendName
            this.objectKey = other.objectKey
            this.createTime = other.createTime
            this.updateTime = other.updateTime
            this.deleted = other.deleted
            this.purgedAt = other.purgedAt
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setBlobId(blobId: String) = apply {
            this.blobId = blobId
        }

        fun setBackendName(backendName: String) = apply {
            this.backendName = backendName
        }

        fun setObjectKey(objectKey: String) = apply {
            this.objectKey = objectKey
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun setDeleted(deleted: Boolean) = apply {
            this.deleted = deleted
        }

        fun setPurgedAt(purgedAt: OffsetDateTime?) = apply {
            this.purgedAt = purgedAt
        }

        fun build(): StorageBlobPlacementEntity {
            return StorageBlobPlacementEntity(
                id = id,
                blobId = blobId,
                backendName = backendName,
                objectKey = objectKey,
                createTime = createTime,
                updateTime = updateTime,
                deleted = deleted,
                purgedAt = purgedAt,
            )
        }
    }

    companion object {
        @JvmStatic
        fun StorageBlobPlacementEntity.toEntity(): StorageBlobPlacementEntity = toBuilder().build()

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
        other as StorageBlobPlacementEntity
        return id != null && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
