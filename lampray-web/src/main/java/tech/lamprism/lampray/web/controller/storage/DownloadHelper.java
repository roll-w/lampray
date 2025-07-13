/*
 * Copyright (C) 2023-2025 RollW
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

import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRange;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author RollW
 */
public final class DownloadHelper {
    public static final String ACCEPT_TYPE = "X-Accept-Type";
    public static final String DISPOSITION_TYPE = "X-Disposition-Type";

    private static final Logger logger = LoggerFactory.getLogger(DownloadHelper.class);

    private static String getEncodedFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static String getResponseType(String mimeType,
                                          HttpServletRequest request) {
        String contentType = request.getHeader(ACCEPT_TYPE);
        if (Strings.isNullOrEmpty(contentType)) {
            return mimeType;
        }
        return contentType;
    }

    private static String getDispositionType(HttpServletRequest request) {
        String dispositionType = request.getHeader(DISPOSITION_TYPE);
        String param = request.getParameter("disposition");
        if (Strings.isNullOrEmpty(dispositionType)) {
            dispositionType = param;
        }
        if (Strings.isNullOrEmpty(dispositionType)) {
            return "attachment";
        }
        return dispositionType;
    }

    /**
     * @deprecated This method is deprecated
     */
    @Deprecated
    public static void downloadFile(FileStorage storage,
                                    String name,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    StorageProvider storageProvider) throws IOException {
        String dispositionType = getDispositionType(request);
        String contentType = getResponseType(storage.getMimeType(), request);
        response.setContentType(contentType);

        if (storage.getFileType() == FileType.TEXT) {
            response.setCharacterEncoding("utf-8");
        }
        List<HttpRange> ranges = HttpRangeUtils.tryGetsRange(request);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Content-Security-Policy", "frame-ancestors 'self' localhost:* 127.0.0.1:*");
        response.setHeader("Content-Disposition",
                dispositionType + ";filename*=utf-8''" + getEncodedFileName(name));
        long length = storage.getFileSize();

        if (!ranges.isEmpty()) {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setHeader("Content-Length", String.valueOf(end - start + 1));
            storageProvider.getFile(
                    storage.getFileId(),
                    response.getOutputStream(),
                    start,
                    end
            );
            return;
        }
        if (length > 0) {
            response.setHeader("Content-Length", String.valueOf(length));
        }
        storageProvider.getFile(storage.getFileId(), response.getOutputStream());
    }

    private DownloadHelper() {
    }
}
