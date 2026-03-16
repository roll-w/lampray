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
import org.springframework.http.HttpRange;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageDownloadSource;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * @author RollW
 */
public final class DownloadHelper {
    public static final String ACCEPT_TYPE = "X-Accept-Type";
    public static final String DISPOSITION_TYPE = "X-Disposition-Type";
    private static final String DISPOSITION_ATTACHMENT = "attachment";
    private static final String DISPOSITION_INLINE = "inline";

    public static void writeDownload(StorageDownloadResult downloadResult,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        if (downloadResult.getMode() != StorageDownloadMode.PROXY) {
            throw new IllegalArgumentException("Download helper only supports proxy download responses.");
        }

        StorageDownloadSource content = downloadResult.getContent();
        if (content == null) {
            throw new IllegalStateException("Proxy download is missing content source.");
        }

        FileStorage fileStorage = downloadResult.getFileStorage();
        String contentType = resolveResponseType(fileStorage.getMimeType(), request);
        String dispositionType = resolveDispositionType(fileStorage, request);
        long length = fileStorage.getFileSize();

        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Content-Security-Policy", "frame-ancestors 'self' localhost:* 127.0.0.1:*");
        response.setHeader(
                "Content-Disposition",
                dispositionType + "; filename=\"" + toAsciiFileName(fileStorage.getFileName())
                        + "\"; filename*=UTF-8''" + encodeFileName(fileStorage.getFileName())
        );
        if (fileStorage.getFileType() == FileType.TEXT) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }

        List<HttpRange> ranges = HttpRangeUtils.tryGetsRange(request);
        if (!ranges.isEmpty() && length > 0) {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setHeader("Content-Length", String.valueOf(end - start + 1));
            content.writeTo(response.getOutputStream(), start, end);
            return;
        }

        if (length > 0) {
            response.setHeader("Content-Length", String.valueOf(length));
        }
        content.writeTo(response.getOutputStream());
    }

    private static String resolveResponseType(String mimeType,
                                              HttpServletRequest request) {
        String requestedType = request.getHeader(ACCEPT_TYPE);
        if (hasText(requestedType)) {
            return requestedType;
        }
        return mimeType;
    }

    private static String resolveDispositionType(FileStorage fileStorage,
                                                 HttpServletRequest request) {
        String requestedDisposition = request.getHeader(DISPOSITION_TYPE);
        if (!hasText(requestedDisposition)) {
            requestedDisposition = request.getParameter("disposition");
        }
        if (hasText(requestedDisposition)) {
            String normalized = requestedDisposition.trim().toLowerCase(Locale.ROOT);
            if (DISPOSITION_INLINE.equals(normalized)) {
                return DISPOSITION_INLINE;
            }
            if (DISPOSITION_ATTACHMENT.equals(normalized)) {
                return DISPOSITION_ATTACHMENT;
            }
        }
        return prefersInline(fileStorage.getFileType()) ? DISPOSITION_INLINE : DISPOSITION_ATTACHMENT;
    }

    private static boolean prefersInline(FileType fileType) {
        return fileType == FileType.IMAGE
                || fileType == FileType.TEXT
                || fileType == FileType.AUDIO
                || fileType == FileType.VIDEO;
    }

    private static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String toAsciiFileName(String fileName) {
        StringBuilder builder = new StringBuilder(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char current = fileName.charAt(i);
            if (current <= 31 || current == '"' || current == '\\' || current == '/' || current == ';' || current > 126) {
                builder.append('_');
                continue;
            }
            builder.append(current);
        }
        String normalized = builder.toString().trim();
        return normalized.isEmpty() ? "download" : normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private DownloadHelper() {
    }
}
