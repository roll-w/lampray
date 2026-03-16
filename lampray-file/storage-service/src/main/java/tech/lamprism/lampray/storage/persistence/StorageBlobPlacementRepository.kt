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
class StorageBlobPlacementRepository(
    private val storageBlobPlacementDao: StorageBlobPlacementDao,
) : CommonRepository<StorageBlobPlacementEntity, String>(storageBlobPlacementDao) {
    fun findAllByBlobId(blobId: String): List<StorageBlobPlacementEntity> {
        return storageBlobPlacementDao.findAll(createBlobIdSpecification(blobId))
    }

    fun findByBlobIdAndBackendName(blobId: String, backendName: String): Optional<StorageBlobPlacementEntity> {
        return storageBlobPlacementDao.findOne(createBlobAndBackendSpecification(blobId, backendName))
    }

    private fun createBlobIdSpecification(blobId: String): Specification<StorageBlobPlacementEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.blobId), blobId)
        }
    }

    private fun createBlobAndBackendSpecification(
        blobId: String,
        backendName: String,
    ): Specification<StorageBlobPlacementEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.blobId), blobId),
                criteriaBuilder.equal(root.get(StorageBlobPlacementEntity_.backendName), backendName),
            )
        }
    }
}
