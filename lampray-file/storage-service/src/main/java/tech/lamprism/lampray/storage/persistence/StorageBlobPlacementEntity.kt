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
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import org.hibernate.proxy.HibernateProxy
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.storage.StorageResourceKind
import tech.lamprism.lampray.storage.domain.StorageBlobPlacementSnapshot
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
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Id
    @Column(name = "placement_id", nullable = false, length = 64, unique = true)
    var placementId: String = "",

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
) : DataEntity<String> {
    override fun getEntityId(): String = placementId

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

    fun lock(): StorageBlobPlacementSnapshot = StorageBlobPlacementSnapshot(
        id = id!!,
        placementId = placementId,
        blobId = blobId,
        backendName = backendName,
        objectKey = objectKey,
        createTime = createTime,
        updateTime = updateTime,
    )

    class Builder {
        private var id: Long? = null
        private var placementId: String? = null
        private var blobId: String = ""
        private var backendName: String = ""
        private var objectKey: String = ""
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        constructor(other: StorageBlobPlacementEntity) {
            id = other.id
            placementId = other.placementId
            blobId = other.blobId
            backendName = other.backendName
            objectKey = other.objectKey
            createTime = other.createTime
            updateTime = other.updateTime
        }

        fun setId(id: Long?) = apply { this.id = id }

        fun setPlacementId(placementId: String) = apply { this.placementId = placementId }

        fun setBlobId(blobId: String) = apply { this.blobId = blobId }

        fun setBackendName(backendName: String) = apply { this.backendName = backendName }

        fun setObjectKey(objectKey: String) = apply { this.objectKey = objectKey }

        fun setCreateTime(createTime: OffsetDateTime) = apply { this.createTime = createTime }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply { this.updateTime = updateTime }

        fun build(): StorageBlobPlacementEntity = StorageBlobPlacementEntity(
            id = id,
            placementId = placementId!!,
            blobId = blobId,
            backendName = backendName,
            objectKey = objectKey,
            createTime = createTime,
            updateTime = updateTime,
        )
    }

    companion object {
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
        return placementId == other.placementId
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
