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

package tech.lamprism.lampray.web.controller.storage.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.query.StorageBackendView;
import tech.lamprism.lampray.storage.query.StorageFileDetails;
import tech.lamprism.lampray.storage.query.StorageFileView;
import tech.lamprism.lampray.storage.query.StorageGroupView;
import tech.lamprism.lampray.storage.query.StorageQueryProvider;
import tech.lamprism.lampray.storage.query.StorageSessionDetails;
import tech.lamprism.lampray.storage.query.StorageSessionView;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.storage.model.StorageAdminFileListRequest;
import tech.lamprism.lampray.web.controller.storage.model.StorageAdminSessionListRequest;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.HttpResponseEntity;

import java.util.List;
import java.util.Locale;

@AdminApi
public class StorageManageController {
    private final StorageQueryProvider storageQueryProvider;

    public StorageManageController(StorageQueryProvider storageQueryProvider) {
        this.storageQueryProvider = storageQueryProvider;
    }

    @GetMapping("/storage/files")
    public HttpResponseEntity<List<StorageFileView>> listFiles(@Valid StorageAdminFileListRequest request) {
        return HttpResponseEntity.success(storageQueryProvider.listFiles(
                request.getPage(),
                request.getSize(),
                request.getGroupName(),
                request.getOwnerUserId(),
                request.getFileName()
        ));
    }

    @GetMapping("/storage/files/{fileId}")
    public HttpResponseEntity<StorageFileDetails> getFile(@PathVariable String fileId) {
        return HttpResponseEntity.success(storageQueryProvider.getFile(fileId));
    }

    @GetMapping("/storage/sessions")
    public HttpResponseEntity<List<StorageSessionView>> listSessions(@Valid StorageAdminSessionListRequest request) {
        return HttpResponseEntity.success(storageQueryProvider.listSessions(
                request.getPage(),
                request.getSize(),
                parseState(request.getStatus()),
                request.getOwnerUserId(),
                request.getFileName()
        ));
    }

    @GetMapping("/storage/sessions/{uploadId}")
    public HttpResponseEntity<StorageSessionDetails> getSession(@PathVariable String uploadId) {
        return HttpResponseEntity.success(storageQueryProvider.getSession(uploadId));
    }

    @GetMapping("/storage/backends")
    public HttpResponseEntity<List<StorageBackendView>> listBackends() {
        return HttpResponseEntity.success(storageQueryProvider.listBackends());
    }

    @GetMapping("/storage/groups")
    public HttpResponseEntity<List<StorageGroupView>> listGroups() {
        return HttpResponseEntity.success(storageQueryProvider.listGroups());
    }

    private StorageUploadSessionState parseState(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return StorageUploadSessionState.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Unknown upload session status: " + rawStatus);
        }
    }
}
