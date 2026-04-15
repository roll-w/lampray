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

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import java.time.OffsetDateTime
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class StorageBlobRepository(
    storageBlobDao: StorageBlobDao,
) : CommonRepository<StorageBlobEntity, String>(storageBlobDao) {
    fun materialize(
        preparedBlob: PreparedBlobMaterialization,
        newBlobId: String,
        now: OffsetDateTime,
    ): StorageBlobEntity {
        val existingBlob = resolveExistingBlob(preparedBlob)
        if (existingBlob == null) {
            val candidate = createBlobCandidate(preparedBlob, newBlobId, now)
            return try {
                save(candidate)
            } catch (exception: DataIntegrityViolationException) {
                findByContentChecksum(preparedBlob.checksum).orElseThrow { exception }
            }
        }
        if (existingBlob.orphanedAt == null) {
            return existingBlob
        }
        val revivedBlob = existingBlob.toBuilder()
            .setOrphanedAt(null)
            .setUpdateTime(now)
            .build()
        return save(revivedBlob)
    }

    fun findByContentChecksum(contentChecksum: String): Optional<StorageBlobEntity> =
        findOne(createChecksumSpecification(contentChecksum))

    fun findAllByOrphanedAtBefore(orphanedAt: OffsetDateTime): List<StorageBlobEntity> =
        findAll(orphanedAtBeforeSpecification(orphanedAt))

    fun existsByPrimaryBackendAndPrimaryObjectKey(primaryBackend: String, objectKey: String): Boolean {
        return findOne(createPrimaryPlacementSpecification(primaryBackend, objectKey)).isPresent
    }

    fun existsOtherByPrimaryBackendAndPrimaryObjectKey(
        primaryBackend: String,
        objectKey: String,
        excludedBlobId: String,
    ): Boolean {
        return findOne(createOtherPrimaryPlacementSpecification(primaryBackend, objectKey, excludedBlobId)).isPresent
    }

    private fun resolveExistingBlob(preparedBlob: PreparedBlobMaterialization): StorageBlobEntity? {
        val existingBlob = preparedBlob.existingBlob
        if (existingBlob == null) {
            return null
        }
        return findById(existingBlob.blobId)
            .or { findByContentChecksum(preparedBlob.checksum) }
            .orElse(null)
    }

    private fun createBlobCandidate(
        preparedBlob: PreparedBlobMaterialization,
        newBlobId: String,
        now: OffsetDateTime,
    ): StorageBlobEntity =
        StorageBlobEntity.builder()
            .setBlobId(newBlobId)
            .setContentChecksum(preparedBlob.checksum)
            .setFileSize(preparedBlob.size)
            .setMimeType(preparedBlob.mimeType)
            .setFileType(preparedBlob.fileType)
            .setPrimaryBackend(preparedBlob.primaryBackend)
            .setPrimaryObjectKey(preparedBlob.primaryObjectKey)
            .setCreateTime(now)
            .setUpdateTime(now)
            .build()

    private fun createChecksumSpecification(contentChecksum: String): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(StorageBlobEntity_.contentChecksum), contentChecksum)
        }
    }

    private fun createPrimaryPlacementSpecification(
        primaryBackend: String,
        objectKey: String,
    ): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageBlobEntity_.primaryBackend), primaryBackend),
                criteriaBuilder.equal(root.get(StorageBlobEntity_.primaryObjectKey), objectKey),
            )
        }
    }

    private fun createOtherPrimaryPlacementSpecification(
        primaryBackend: String,
        objectKey: String,
        excludedBlobId: String,
    ): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageBlobEntity_.primaryBackend), primaryBackend),
                criteriaBuilder.equal(root.get(StorageBlobEntity_.primaryObjectKey), objectKey),
                criteriaBuilder.notEqual(root.get(StorageBlobEntity_.blobId), excludedBlobId),
            )
        }
    }

    private fun orphanedAtBeforeSpecification(orphanedAt: OffsetDateTime): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.lessThanOrEqualTo(root.get(StorageBlobEntity_.orphanedAt), orphanedAt)
        }
    }
}
