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

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.storage.session.UploadSessionStatus
import tech.rollw.common.web.page.Page
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Repository
class StorageUploadSessionRepository(
    uploadSessionDao: StorageUploadSessionDao,
) : CommonRepository<StorageUploadSessionEntity, String>(uploadSessionDao) {
    fun findAll(
        pageable: Pageable,
        specification: Specification<StorageUploadSessionEntity>,
    ): Page<StorageUploadSessionEntity> = super.findAll(pageable, specification)

    fun findAllByStatus(status: UploadSessionStatus): List<StorageUploadSessionEntity> =
        findAll(statusSpec(status))

    fun findAllByStatusAndExpiresAtBefore(
        status: UploadSessionStatus,
        expiresAt: OffsetDateTime,
    ): List<StorageUploadSessionEntity> =
        findAll(statusSpec(status).and(expiresAtBeforeSpec(expiresAt)))

    fun findAllByStatusAndUpdateTimeBefore(
        status: UploadSessionStatus,
        updateTime: OffsetDateTime,
    ): List<StorageUploadSessionEntity> =
        findAll(statusSpec(status).and(updateTimeBeforeSpec(updateTime)))

    fun existsOtherActiveSessionByPrimaryBackendAndObjectKey(
        primaryBackend: String,
        objectKey: String,
        excludedUploadId: String,
    ): Boolean = findOne(activeObjectReferenceSpec(primaryBackend, objectKey, excludedUploadId)).isPresent

    private fun statusSpec(status: UploadSessionStatus): Specification<StorageUploadSessionEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), status)
        }

    private fun expiresAtBeforeSpec(expiresAt: OffsetDateTime): Specification<StorageUploadSessionEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.lessThan(root.get(StorageUploadSessionEntity_.expiresAt), expiresAt)
        }

    private fun updateTimeBeforeSpec(updateTime: OffsetDateTime): Specification<StorageUploadSessionEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.lessThan(root.get(StorageUploadSessionEntity_.updateTime), updateTime)
        }

    private fun activeObjectReferenceSpec(
        primaryBackend: String,
        objectKey: String,
        excludedUploadId: String,
    ): Specification<StorageUploadSessionEntity> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.primaryBackend), primaryBackend),
                criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.objectKey), objectKey),
                criteriaBuilder.notEqual(root.get(StorageUploadSessionEntity_.uploadId), excludedUploadId),
                criteriaBuilder.notEqual(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.EXPIRED),
            )
        }
}
