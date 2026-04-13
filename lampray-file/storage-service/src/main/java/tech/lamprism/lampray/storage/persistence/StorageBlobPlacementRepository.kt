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
import java.time.OffsetDateTime
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class StorageBlobPlacementRepository(
    storageBlobPlacementDao: StorageBlobPlacementDao,
) : CommonRepository<StorageBlobPlacementEntity, Long>(storageBlobPlacementDao) {
    fun syncPlacements(
        blobId: String,
        placements: Map<String, String>,
        now: OffsetDateTime,
    ) {
        placements.forEach { (backendName, objectKey) ->
            upsertPlacement(blobId, backendName, objectKey, now)
        }
    }

    fun findAllByBlobId(blobId: String): List<StorageBlobPlacementEntity> {
        return findAll(createBlobIdSpecification(blobId, false))
    }

    fun findAllIncludingDeletedByBlobId(blobId: String): List<StorageBlobPlacementEntity> {
        return findAll(createBlobIdSpecification(blobId, null))
    }

    fun findByBlobIdAndBackendName(blobId: String, backendName: String): Optional<StorageBlobPlacementEntity> {
        return findOne(createBlobAndBackendSpecification(blobId, backendName, false))
    }

    fun findAnyByBlobIdAndBackendName(blobId: String, backendName: String): Optional<StorageBlobPlacementEntity> {
        return findOne(createBlobAndBackendSpecification(blobId, backendName, null))
    }

    fun existsByBackendNameAndObjectKey(backendName: String, objectKey: String): Boolean {
        return findOne(createBackendAndObjectKeySpecification(backendName, objectKey, false)).isPresent
    }

    private fun upsertPlacement(
        blobId: String,
        backendName: String,
        objectKey: String,
        now: OffsetDateTime,
    ) {
        if (findByBlobIdAndBackendName(blobId, backendName).isPresent) {
            return
        }
        val existingPlacement = findAnyByBlobIdAndBackendName(blobId, backendName).orElse(null)
        if (existingPlacement != null) {
            save(
                existingPlacement.toBuilder()
                    .setObjectKey(objectKey)
                    .setDeleted(false)
                    .setUpdateTime(now)
                    .build(),
            )
            return
        }
        val placementEntity = StorageBlobPlacementEntity.builder()
            .setBlobId(blobId)
            .setBackendName(backendName)
            .setObjectKey(objectKey)
            .setCreateTime(now)
            .setUpdateTime(now)
            .setDeleted(false)
            .build()
        try {
            save(placementEntity)
        } catch (exception: DataIntegrityViolationException) {
            val conflictedPlacement = findAnyByBlobIdAndBackendName(blobId, backendName).orElse(null) ?: throw exception
            save(
                conflictedPlacement.toBuilder()
                    .setObjectKey(objectKey)
                    .setDeleted(false)
                    .setUpdateTime(now)
                    .build(),
            )
        }
    }

    private fun createBlobIdSpecification(
        blobId: String,
        deleted: Boolean?
    ): Specification<StorageBlobPlacementEntity> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf(
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.blobId), blobId),
            )
            if (deleted != null) {
                predicates.add(criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.deleted), deleted))
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun createBlobAndBackendSpecification(
        blobId: String,
        backendName: String,
        deleted: Boolean?,
    ): Specification<StorageBlobPlacementEntity> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf(
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.blobId), blobId),
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.backendName), backendName),
            )
            if (deleted != null) {
                predicates.add(criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.deleted), deleted))
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    private fun createBackendAndObjectKeySpecification(
        backendName: String,
        objectKey: String,
        deleted: Boolean?,
    ): Specification<StorageBlobPlacementEntity> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf(
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.backendName), backendName),
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.objectKey), objectKey),
            )
            if (deleted != null) {
                predicates.add(criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.deleted), deleted))
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
