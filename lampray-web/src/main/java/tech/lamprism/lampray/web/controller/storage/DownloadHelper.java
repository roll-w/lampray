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
import tech.lamprism.lampray.storage.StorageByteRange;
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
        String contentType = normalizeMimeType(fileStorage.getMimeType());
        String dispositionType = resolveDispositionType(fileStorage, contentType, request);
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

        RangeRequestResolution rangeResolution = resolveRange(request, response, length);
        if (rangeResolution.isRejected()) {
            return;
        }

        StorageByteRange resolvedRange = rangeResolution.range();
        if (resolvedRange != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader(
                    "Content-Range",
                    "bytes " + resolvedRange.startBytes() + "-" + resolvedRange.endBytes() + "/" + length
            );
            response.setHeader("Content-Length", String.valueOf(resolvedRange.length()));
            content.transferTo(response.getOutputStream(), resolvedRange);
            return;
        }

        if (length > 0) {
            response.setHeader("Content-Length", String.valueOf(length));
        }
        content.transferTo(response.getOutputStream());
    }

    private static RangeRequestResolution resolveRange(HttpServletRequest request,
                                                       HttpServletResponse response,
                                                       long length) {
        List<HttpRange> ranges;
        try {
            ranges = HttpRangeUtils.tryGetsRange(request);
        } catch (IllegalArgumentException exception) {
            return rejectRange(response, length);
        }
        if (ranges.isEmpty()) {
            return RangeRequestResolution.none();
        }
        if (length <= 0) {
            return rejectRange(response, length);
        }

        HttpRange range = ranges.get(0);
        try {
            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            if (start >= length || end >= length) {
                return rejectRange(response, length);
            }
            return RangeRequestResolution.resolved(new StorageByteRange(start, end));
        } catch (IllegalArgumentException exception) {
            return rejectRange(response, length);
        }
    }

    private static RangeRequestResolution rejectRange(HttpServletResponse response,
                                                      long length) {
        response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setHeader("Content-Range", "bytes */" + Math.max(length, 0));
        response.setHeader("Content-Length", "0");
        return RangeRequestResolution.rejected();
    }

    private record RangeRequestResolution(StorageByteRange range, boolean isRejected) {
        private static RangeRequestResolution none() {
            return new RangeRequestResolution(null, false);
        }

        private static RangeRequestResolution resolved(StorageByteRange range) {
            return new RangeRequestResolution(range, false);
        }

        private static RangeRequestResolution rejected() {
            return new RangeRequestResolution(null, true);
        }
    }

    private static String resolveDispositionType(FileStorage fileStorage,
                                                 String mimeType,
                                                 HttpServletRequest request) {
        String requestedDisposition = request.getHeader(DISPOSITION_TYPE);
        if (!hasText(requestedDisposition)) {
            requestedDisposition = request.getParameter("disposition");
        }
        boolean inlineAllowed = supportsInlinePreview(fileStorage.getFileType(), mimeType);
        if (hasText(requestedDisposition)) {
            String normalized = requestedDisposition.trim().toLowerCase(Locale.ROOT);
            if (DISPOSITION_INLINE.equals(normalized) && inlineAllowed) {
                return DISPOSITION_INLINE;
            }
            if (DISPOSITION_ATTACHMENT.equals(normalized)) {
                return DISPOSITION_ATTACHMENT;
            }
        }
        return inlineAllowed ? DISPOSITION_INLINE : DISPOSITION_ATTACHMENT;
    }

    private static boolean supportsInlinePreview(FileType fileType,
                                                 String mimeType) {
        if (isUnsafeInlineMimeType(mimeType)) {
            return false;
        }
        if (fileType == FileType.IMAGE) {
            return mimeType.startsWith("image/");
        }
        if (fileType == FileType.AUDIO) {
            return mimeType.startsWith("audio/");
        }
        if (fileType == FileType.VIDEO) {
            return mimeType.startsWith("video/");
        }
        return "text/plain".equals(mimeType);
    }

    private static boolean isUnsafeInlineMimeType(String mimeType) {
        return "text/html".equals(mimeType)
                || "application/xhtml+xml".equals(mimeType)
                || "image/svg+xml".equals(mimeType)
                || "text/xml".equals(mimeType)
                || "application/xml".equals(mimeType)
                || mimeType.endsWith("+xml")
                || "text/javascript".equals(mimeType)
                || "application/javascript".equals(mimeType)
                || "application/json".equals(mimeType);
    }

    private static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String normalizeMimeType(String mimeType) {
        if (!hasText(mimeType)) {
            return "application/octet-stream";
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int parameterIndex = normalized.indexOf(';');
        if (parameterIndex >= 0) {
            normalized = normalized.substring(0, parameterIndex).trim();
        }
        return normalized.isEmpty() ? "application/octet-stream" : normalized;
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
