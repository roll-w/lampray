/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.web.controller.resource;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLConnection;

/**
 * @author RollW
 */
@RestController
public class ResourceController {

    @GetMapping("/static/{*path}")
    public void getResource(@PathVariable("path") String path,
                            HttpServletResponse response) throws IOException {
        String normalizedPath = normalizeStaticPath(path);
        if (normalizedPath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ClassPathResource classPathResource =
                new ClassPathResource("static" + normalizedPath);
        if (!classPathResource.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = URLConnection.guessContentTypeFromName(classPathResource.getFilename());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        response.setStatus(200);
        response.setContentType(mimeType);
        ServletOutputStream outputStream = response.getOutputStream();
        try (var inputStream = classPathResource.getInputStream()) {
            inputStream.transferTo(outputStream);
        }
    }

    private String normalizeStaticPath(String path) {
        if (path == null || path.isBlank() || path.indexOf('\\') >= 0) {
            return null;
        }

        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String[] segments = normalizedPath.split("/");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (".".equals(segment) || "..".equals(segment)) {
                return null;
            }
            builder.append('/').append(segment);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

}
