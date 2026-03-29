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

package tech.lamprism.lampray.storage.facade;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Service
public class StorageUploadOperationRouter {
    private final Map<StorageUploadMode, StorageUploadStrategy> uploadStrategies;

    public StorageUploadOperationRouter(List<StorageUploadStrategy> uploadStrategies) {
        this.uploadStrategies = Maps.uniqueIndex(uploadStrategies, StorageUploadStrategy::mode);
    }

    public FileStorage uploadContent(StorageUploadSessionEntity uploadSession,
                                     InputStream inputStream) throws IOException {
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadSession.getUploadId());
        }
        return requireStrategy(uploadSession.getUploadMode()).uploadContent(uploadSession, inputStream);
    }

    public FileStorage completeUpload(StorageUploadSessionEntity uploadSession) throws IOException {
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires proxy upload: " + uploadSession.getUploadId());
        }
        return requireStrategy(uploadSession.getUploadMode()).completeUpload(uploadSession);
    }

    private StorageUploadStrategy requireStrategy(StorageUploadMode mode) {
        StorageUploadStrategy strategy = uploadStrategies.get(mode);
        if (strategy == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Unsupported upload mode: " + mode);
        }
        return strategy;
    }
}
