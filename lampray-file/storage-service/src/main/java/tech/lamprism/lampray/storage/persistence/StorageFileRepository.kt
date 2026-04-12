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
class StorageFileRepository(
    storageFileDao: StorageFileDao,
) : CommonRepository<StorageFileEntity, String>(storageFileDao) {
    fun findActiveById(fileId: String): Optional<StorageFileEntity> =
        findOne(activeFileSpecification(fileId))

    fun existsActiveByBlobId(blobId: String): Boolean =
        findOne(activeBlobIdSpecification(blobId)).isPresent

    fun findDeletedByBlobId(blobId: String): List<StorageFileEntity> =
        findAll(deletedBlobIdSpecification(blobId))

    private fun activeFileSpecification(fileId: String): Specification<StorageFileEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageFileEntity_.fileId), fileId),
                criteriaBuilder.isFalse(root.get(StorageFileEntity_.deleted)),
            )
        }

    private fun activeBlobIdSpecification(blobId: String): Specification<StorageFileEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageFileEntity_.blobId), blobId),
                criteriaBuilder.isFalse(root.get(StorageFileEntity_.deleted)),
            )
        }

    private fun deletedBlobIdSpecification(blobId: String): Specification<StorageFileEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageFileEntity_.blobId), blobId),
                criteriaBuilder.isTrue(root.get(StorageFileEntity_.deleted)),
            )
        }
}
