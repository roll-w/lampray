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
import tech.lamprism.lampray.storage.StorageVisibility
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "storage_file")
class StorageFileEntity(
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Id
    @Column(name = "file_id", nullable = false, length = 64, unique = true)
    var fileId: String = "",

    @Column(name = "blob_id", nullable = false, length = 64)
    var blobId: String = "",

    @Column(name = "group_name", nullable = false, length = 80)
    var groupName: String = "",

    @Column(name = "owner_user_id")
    var ownerUserId: Long? = null,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = "",

    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,

    @Column(name = "mime_type", nullable = false, length = 255)
    var mimeType: String = "application/octet-stream",

    @Column(name = "file_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var fileType: FileType = FileType.OTHER,

    @Column(name = "visibility", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var visibility: StorageVisibility = StorageVisibility.PRIVATE,

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),
) : DataEntity<String> {
    override fun getEntityId(): String = fileId

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    override fun getSystemResourceKind(): SystemResourceKind = StorageResourceKind

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val otherClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisClass = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisClass != otherClass) return false
        other as StorageFileEntity
        return fileId == other.fileId
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
