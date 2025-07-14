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

package tech.lamprism.lampray.web.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tech.lamprism.lampray.LampException;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ResourceConfigKeys;
import tech.lamprism.lampray.web.configuration.LocalConfigConfiguration;
import tech.rollw.common.web.CommonErrorCode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;

/**
 * @author RollW
 */
@Controller
public class FrontendResourceController {
    private static final Logger logger = LoggerFactory.getLogger(FrontendResourceController.class);

    private static final String ASSETS_PATH = "/assets/";

    private static final String CONFIG_JS_TEMPLATE =
            "window.config={server:{" +
                    "host:\"{host}\"," +
                    "httpProtocol:\"{httpProtocol}\"," +
                    "wsProtocol:\"{wsProtocol}\"" +
                    "}}";

    private final ClassPathResource index = new ClassPathResource(ASSETS_PATH + "index.html");
    private final ConfigReader configReader;

    public FrontendResourceController(
            @Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER) ConfigReader configReader) {
        this.configReader = configReader;
    }

    @GetMapping(value = "/{*path}")
    public void servingResource(HttpServletRequest request,
                                HttpServletResponse response,
                                @PathVariable("path") String path) throws IOException {
        // This will not be null, because the spec has a default value
        @SuppressWarnings("DataFlowIssue")
        boolean enabled = configReader.get(ResourceConfigKeys.FRONTEND_ENABLED);
        if (!enabled) {
            throw new LampException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        String removed = removePrefix(path);
        if (onConfigJs(removed, request, response)) {
            return;
        }
        ServletOutputStream outputStream = response.getOutputStream();
        if (removed.isEmpty()) {
            fallback(response);
            return;
        }
        String extension = FilenameUtils.getExtension(removed);
        if (StringUtils.isEmpty(extension)) {
            fallback(response);
            return;
        }
        Resource resource = loadResource(removed);
        if (!resource.exists()) {
            fallback(response);
            return;
        }
        logger.debug("Serving frontend resource: '{}'", path);
        setContentType(response, removed);
        resource.getInputStream().transferTo(outputStream);
    }

    private boolean onConfigJs(String path,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        if (!path.equals("config.js")) {
            return false;
        }
        // TODO: make this configurable
        String host = toAddress(request);
        String httpProtocol = request.getScheme();
        String wsProtocol = httpProtocol.equals("https") ? "wss" : "ws";
        String configJs = CONFIG_JS_TEMPLATE
                .replace("{host}", host)
                .replace("{httpProtocol}", httpProtocol)
                .replace("{wsProtocol}", wsProtocol);
        response.setContentType("text/javascript");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(configJs.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private String toAddress(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if (serverPort == 80 && "http".equals(scheme) ||
                serverPort == 443 && "https".equals(scheme)) {
            return serverName;
        }
        return serverName + ":" + serverPort;
    }

    private Resource loadResource(String path) {
        String resourceSource = configReader.get(ResourceConfigKeys.RESOURCE_SOURCE);
        if (Objects.equals(resourceSource, ResourceConfigKeys.EMBEDDED_RESOURCE)) {
            return loadEmbeddedResource(path);
        }
        return loadFileResource(path);
    }

    private Resource loadEmbeddedResource(String path) {
        if (!index.exists()) {
            throw new LampException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        ClassPathResource accessed = new ClassPathResource(ASSETS_PATH + path);
        if (!accessed.exists()) {
            return index;
        }
        return accessed;
    }

    // private final Map<String, FileSystemResource> resourceCache = new ConcurrentHashMap<>();

    private Resource loadFileResource(String path) {
        FileSystemResource index = new FileSystemResource(
                new File(configReader.get(ResourceConfigKeys.RESOURCE_SOURCE),
                        "index.html")
        );
        if (!index.exists()) {
            throw new LampException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        FileSystemResource accessed = new FileSystemResource(
                new File(configReader.get(ResourceConfigKeys.RESOURCE_SOURCE), path)
        );
        if (!accessed.exists()) {
            return index;
        }
        return accessed;
    }

    private String removePrefix(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private void setContentType(HttpServletResponse response, String path) {
        MediaTypeFactory.getMediaType(path).ifPresent(mediaType ->
                response.setContentType(mediaType.toString())
        );
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofDays(30));
        // TODO: make this configurable
        response.setHeader("Cache-Control", cacheControl.getHeaderValue());
        response.setHeader("Expires", new Date(System.currentTimeMillis() + 86400000).toString());
        response.setHeader("Vary", "Accept-Encoding");
    }

    private void fallback(HttpServletResponse response) throws IOException {
        Resource indexHtml = getIndexHtml();
        if (!indexHtml.exists()) {
            throw new LampException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        indexHtml.getInputStream().transferTo(response.getOutputStream());
    }

    private Resource getIndexHtml() {
        String source = configReader.get(ResourceConfigKeys.RESOURCE_SOURCE);
        if (Objects.equals(source, ResourceConfigKeys.EMBEDDED_RESOURCE)) {
            return index;
        }
        return new FileSystemResource(new File(source, "index.html"));
    }

}