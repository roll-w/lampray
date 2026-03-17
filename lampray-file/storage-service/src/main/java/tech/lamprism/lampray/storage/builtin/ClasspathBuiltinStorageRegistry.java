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

package tech.lamprism.lampray.storage.builtin;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.TimeAttributed;
import tech.lamprism.lampray.storage.DefaultStorageIds;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.source.InputStreamDownloadSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public class ClasspathBuiltinStorageRegistry implements BuiltinStorageRegistry {
    private static final String MIME_TYPE_SVG = "image/svg+xml";

    private final Map<String, BuiltinStorageResource> resources;

    public ClasspathBuiltinStorageRegistry() {
        this.resources = Map.of(
                DefaultStorageIds.DEFAULT_AVATAR_ID,
                create(
                        "storage/builtin/default-avatar.svg",
                        DefaultStorageIds.DEFAULT_AVATAR_ID,
                        "default-avatar.svg",
                        "static/images/default-avatar.svg",
                        "/static/images/default-avatar.svg"
                ),
                DefaultStorageIds.DEFAULT_USER_COVER_ID,
                create(
                        "storage/builtin/default-cover.svg",
                        DefaultStorageIds.DEFAULT_USER_COVER_ID,
                        "default-user-cover.svg",
                        "static/images/default-cover.svg",
                        "/static/images/default-cover.svg"
                ),
                DefaultStorageIds.DEFAULT_ARTICLE_COVER_ID,
                create(
                        "storage/builtin/default-cover.svg",
                        DefaultStorageIds.DEFAULT_ARTICLE_COVER_ID,
                        "default-article-cover.svg",
                        "static/images/default-cover.svg",
                        "/static/images/default-cover.svg"
                ),
                DefaultStorageIds.DEFAULT_CATEGORY_COVER_ID,
                create(
                        "storage/builtin/default-cover.svg",
                        DefaultStorageIds.DEFAULT_CATEGORY_COVER_ID,
                        "default-category-cover.svg",
                        "static/images/default-cover.svg",
                        "/static/images/default-cover.svg"
                ),
                DefaultStorageIds.DEFAULT_LOGO_ID,
                create(
                        "storage/builtin/default-logo.svg",
                        DefaultStorageIds.DEFAULT_LOGO_ID,
                        "default-logo.svg",
                        "static/images/default-logo.svg",
                        "/static/images/default-logo.svg"
                )
        );
    }

    @Override
    public boolean contains(String fileId) {
        return resources.containsKey(fileId);
    }

    @Override
    public BuiltinStorageResource get(String fileId) {
        BuiltinStorageResource resource = resources.get(fileId);
        if (resource == null) {
            throw new IllegalArgumentException("Unknown builtin storage resource: " + fileId);
        }
        return resource;
    }

    private BuiltinStorageResource create(String classPathLocation,
                                          String fileId,
                                          String fileName,
                                          String staticClassPathLocation,
                                          String publicUrlPath) {
        Resource resource = new ClassPathResource(classPathLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing builtin storage resource: " + classPathLocation);
        }
        return new BuiltinStorageResource(
                FileStorage.builder()
                        .setFileId(fileId)
                        .setFileName(fileName)
                        .setFileSize(resolveSize(resource))
                        .setMimeType(MIME_TYPE_SVG)
                        .setFileType(FileType.IMAGE)
                        .setCreateTime(TimeAttributed.NONE_TIME)
                        .build(),
                createDownloadSource(resource),
                resolvePublicUrlPath(staticClassPathLocation, publicUrlPath)
        );
    }

    private InputStreamDownloadSource createDownloadSource(Resource resource) {
        try {
            return InputStreamDownloadSource.fromPath(resource.getFile().toPath());
        } catch (IOException exception) {
            return InputStreamDownloadSource.from(resource::getInputStream);
        }
    }

    private String resolvePublicUrlPath(String classPathLocation,
                                        String publicUrlPath) {
        Resource resource = new ClassPathResource(classPathLocation);
        return resource.exists() ? publicUrlPath : null;
    }

    private long resolveSize(Resource resource) {
        try {
            return resource.contentLength();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to resolve builtin resource size", exception);
        }
    }
}
