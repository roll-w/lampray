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

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class StorageBlobRepository(
    private val storageBlobDao: StorageBlobDao,
) : CommonRepository<StorageBlobEntity, String>(storageBlobDao) {
    fun findByChecksumSha256(checksumSha256: String): Optional<StorageBlobEntity> {
        return storageBlobDao.findOne(createChecksumSpecification(checksumSha256))
    }

    fun existsByPrimaryBackendAndPrimaryObjectKey(primaryBackend: String, objectKey: String): Boolean {
        return storageBlobDao.findOne(createPrimaryPlacementSpecification(primaryBackend, objectKey)).isPresent
    }

    private fun createChecksumSpecification(checksumSha256: String): Specification<StorageBlobEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(StorageBlobEntity_.checksumSha256), checksumSha256)
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
}
