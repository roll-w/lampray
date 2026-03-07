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

package tech.lamprism.lampray.content.service;

import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentProvider;
import tech.lamprism.lampray.content.ContentProviderFactory;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.SimpleContentIdentity;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceKind;
import tech.rollw.common.web.system.SystemResourceOperator;
import tech.rollw.common.web.system.SystemResourceOperatorFactory;
import tech.rollw.common.web.system.SystemResourceProvider;
import tech.rollw.common.web.system.UnsupportedKindException;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class ContentProvideService implements SystemResourceProvider<String>,
        SystemResourceOperatorFactory<String>, ContentProviderFactory {
    private final List<ContentProvider> contentProviders;

    public ContentProvideService(List<ContentProvider> contentProviders) {
        this.contentProviders = contentProviders;
    }

    @Override
    public boolean supports(@NonNull SystemResourceKind systemResourceKind) {
        return ContentType.from(systemResourceKind) != null;
    }

    @NonNull
    @Override
    public SystemResourceOperator<String> createResourceOperator(
            SystemResource<String> systemResource,
            boolean checkDelete) {
        ContentType contentType = ContentType.from(systemResource.getSystemResourceKind());
        if (contentType == null) {
            throw new UnsupportedKindException("Unsupported kind: " + systemResource.getSystemResourceKind());
        }
        ContentProvider contentProvider = findProvider(contentType);
        SimpleContentIdentity contentIdentity =
                new SimpleContentIdentity(systemResource.getResourceId(), contentType);
        return contentProvider.getContentOperator(contentIdentity, checkDelete);
    }

    @NonNull
    @Override
    public ContentDetails provide(@NonNull String resourceId,
                                  @NonNull SystemResourceKind systemResourceKind)
            throws CommonRuntimeException, UnsupportedKindException {
        ContentType contentType = ContentType.from(systemResourceKind);
        if (contentType == null) {
            throw new UnsupportedKindException("Unsupported kind: " + systemResourceKind);
        }
        ContentProvider contentProvider = findProvider(contentType);
        SimpleContentIdentity contentIdentity =
                new SimpleContentIdentity(resourceId, contentType);
        return contentProvider.getContentDetails(contentIdentity);
    }

    @NonNull
    @Override
    public ContentDetails provide(@NonNull SystemResource<String> rawSystemResource)
            throws CommonRuntimeException, UnsupportedKindException {
        if (rawSystemResource instanceof ContentDetails contentDetails) {
            return contentDetails;
        }
        return provide(rawSystemResource.getResourceId(), rawSystemResource.getSystemResourceKind());
    }

    private ContentProvider findProvider(ContentType contentType) {
        return contentProviders.stream()
                .filter(provider -> provider.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedKindException(
                        "No provider founds support the content type: " + contentType));
    }

    @Override
    public ContentProvider getContentProvider(ContentType contentType) {
        return findProvider(contentType);
    }
}
