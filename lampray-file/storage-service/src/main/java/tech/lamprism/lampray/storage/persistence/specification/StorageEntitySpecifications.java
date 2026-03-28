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

package tech.lamprism.lampray.storage.persistence.specification;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity_;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity_;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

public final class StorageEntitySpecifications {
    private StorageEntitySpecifications() {
    }

    public static Specification<StorageFileEntity> files(String groupName,
                                                         Long ownerUserId,
                                                         String fileName) {
        return combine(
                eqIfPresent(StorageFileEntity_.groupName, StringUtils.trimToNull(groupName)),
                eqIfPresent(StorageFileEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageFileEntity_.fileName, fileName)
        );
    }

    public static Specification<StorageUploadSessionEntity> sessions(StorageUploadSessionState state,
                                                                     Long ownerUserId,
                                                                     String fileName,
                                                                     OffsetDateTime now) {
        return combine(
                trackedStateSpec(state, now),
                eqIfPresent(StorageUploadSessionEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageUploadSessionEntity_.fileName, fileName)
        );
    }

    private static Specification<StorageUploadSessionEntity> trackedStateSpec(StorageUploadSessionState state,
                                                                              OffsetDateTime now) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case PENDING -> (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.PENDING),
                    criteriaBuilder.greaterThanOrEqualTo(root.get(StorageUploadSessionEntity_.expiresAt), now)
            );
            case COMPLETED -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.COMPLETED);
            case EXPIRED -> (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.EXPIRED),
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.PENDING),
                            criteriaBuilder.lessThan(root.get(StorageUploadSessionEntity_.expiresAt), now)
                    )
            );
        };
    }

    @SafeVarargs
    private static <T> Specification<T> combine(Specification<T>... specifications) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> specification : specifications) {
            if (specification != null) {
                result = result.and(specification);
            }
        }
        return result;
    }

    private static <T, V> Specification<T> eqIfPresent(jakarta.persistence.metamodel.SingularAttribute<T, V> attribute,
                                                       V value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(attribute), value);
    }

    private static <T> Specification<T> likeIfPresent(jakarta.persistence.metamodel.SingularAttribute<T, String> attribute,
                                                      String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(attribute)), normalized);
    }
}
