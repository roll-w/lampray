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

package tech.lamprism.lampray.web.controller.storage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.controller.Api;
import tech.lamprism.lampray.web.controller.storage.model.StorageFileSummaryResponse;
import tech.lamprism.lampray.web.controller.storage.model.StorageUploadCreateRequest;
import tech.lamprism.lampray.web.controller.storage.model.StorageUploadSessionResponse;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.system.ContextThreadAware;

import java.io.IOException;

/**
 * @author RollW
 */
@Api
public class StorageController {
    private final StorageProvider storageProvider;
    private final ContextThreadAware<ApiContext> apiContextThreadAware;

    public StorageController(StorageProvider storageProvider,
                             ContextThreadAware<ApiContext> apiContextThreadAware) {
        this.storageProvider = storageProvider;
        this.apiContextThreadAware = apiContextThreadAware;
    }

    @PostMapping("/files/uploads")
    public HttpResponseEntity<StorageUploadSessionResponse> createUploadSession(
            @RequestBody StorageUploadCreateRequest request) throws IOException {
        Long userId = requireUserId();
        StorageUploadSession uploadSession = storageProvider.createUploadSession(
                new StorageUploadRequest(
                        request.groupName(),
                        request.fileName(),
                        request.size(),
                        request.mimeType(),
                        request.checksumSha256()
                ),
                userId
        );
        return HttpResponseEntity.success(StorageUploadSessionResponse.from(uploadSession));
    }

    @PutMapping("/files/uploads/{uploadId}/content")
    public HttpResponseEntity<StorageFileSummaryResponse> uploadFileContent(
            @PathVariable String uploadId,
            HttpServletRequest request) throws IOException {
        Long userId = requireUserId();
        FileStorage fileStorage = storageProvider.uploadFileContent(uploadId, request.getInputStream(), userId);
        return HttpResponseEntity.success(StorageFileSummaryResponse.from(fileStorage));
    }

    @PostMapping("/files/uploads/{uploadId}:complete")
    public HttpResponseEntity<StorageFileSummaryResponse> completeUpload(
            @PathVariable String uploadId) throws IOException {
        Long userId = requireUserId();
        FileStorage fileStorage = storageProvider.completeUpload(uploadId, userId);
        return HttpResponseEntity.success(StorageFileSummaryResponse.from(fileStorage));
    }

    @GetMapping("/files/{fileId}")
    public void getFile(@PathVariable String fileId,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        serveDownload(fileId, request, response);
    }

    @GetMapping("/storages/{id}")
    public void getStorage(@PathVariable String id,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        serveDownload(id, request, response);
    }

    private void serveDownload(String fileId,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        Long userId = currentUserId();
        StorageDownloadResult downloadResult = storageProvider.resolveDownload(fileId, userId);
        if (downloadResult.getMode() == StorageDownloadMode.DIRECT) {
            StorageAccessRequest directRequest = downloadResult.getDirectRequest();
            if (directRequest == null) {
                throw new IllegalStateException("Direct download is missing access request.");
            }
            if (!"GET".equalsIgnoreCase(directRequest.getMethod()) || !directRequest.getHeaders().isEmpty()) {
                throw new IllegalStateException("Direct download requires a simple GET request.");
            }
            response.sendRedirect(directRequest.getUrl());
            return;
        }

        try {
            DownloadHelper.writeDownload(downloadResult, request, response);
        } catch (IOException e) {
            response.reset();
            throw e;
        }
    }

    private Long requireUserId() {
        ApiContext apiContext = apiContextThreadAware.getContextThread().getContext();
        if (apiContext == null || apiContext.getUser() == null) {
            throw new tech.lamprism.lampray.storage.StorageException(
                    AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "Authentication is required."
            );
        }
        return apiContext.getUser().getUserId();
    }

    private Long currentUserId() {
        ApiContext apiContext = apiContextThreadAware.getContextThread().getContext();
        if (apiContext == null || apiContext.getUser() == null) {
            return null;
        }
        return apiContext.getUser().getUserId();
    }
}
