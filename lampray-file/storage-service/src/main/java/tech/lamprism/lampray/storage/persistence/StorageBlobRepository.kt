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

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import java.time.OffsetDateTime
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class StorageBlobRepository(
    private val storageBlobDao: StorageBlobDao,
    private val entityManager: EntityManager,
) : CommonRepository<StorageBlobEntity, String>(storageBlobDao) {
    fun findByContentChecksum(contentChecksum: String): Optional<StorageBlobEntity> {
        return storageBlobDao.findOne(createChecksumSpecification(contentChecksum))
    }

    fun findAllByOrphanedAtBefore(orphanedAt: OffsetDateTime): List<StorageBlobEntity> =
        findAll(orphanedAtBeforeSpecification(orphanedAt))

    fun lockById(blobId: String): Optional<StorageBlobEntity> =
        Optional.ofNullable(entityManager.find(StorageBlobEntity::class.java, blobId, LockModeType.PESSIMISTIC_WRITE))

    fun existsByPrimaryBackendAndPrimaryObjectKey(primaryBackend: String, objectKey: String): Boolean {
        return storageBlobDao.findOne(createPrimaryPlacementSpecification(primaryBackend, objectKey)).isPresent
    }

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

    private fun orphanedAtBeforeSpecification(orphanedAt: OffsetDateTime): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.lessThanOrEqualTo(root.get(StorageBlobEntity_.orphanedAt), orphanedAt)
        }
    }
}
