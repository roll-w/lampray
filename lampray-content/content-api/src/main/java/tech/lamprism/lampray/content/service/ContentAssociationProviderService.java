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

package tech.lamprism.lampray.content.service;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.content.ContentAssociated;
import tech.lamprism.lampray.content.ContentAssociationProvider;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentProviderFactory;
import tech.lamprism.lampray.content.ContentType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class ContentAssociationProviderService implements ContentAssociationProvider {
    private final ContentProviderFactory contentProviderFactory;

    public ContentAssociationProviderService(ContentProviderFactory contentProviderFactory) {
        this.contentProviderFactory = contentProviderFactory;
    }

    @Override
    public ContentDetails getAssociatedContentDetails(ContentAssociated contentAssociated) {
        ContentIdentity associatedContent = contentAssociated.getAssociatedContent();
        return contentProviderFactory.getContentProvider(associatedContent.getContentType())
                .getContentDetails(associatedContent);
    }

    @Override
    public List<? extends ContentDetails> getAssociatedContentDetails(List<ContentAssociated> contentAssociated) {
        Map<ContentType, List<ContentIdentity>> contentMap = contentAssociated.stream()
                .map(ContentAssociated::getAssociatedContent)
                .collect(Collectors.groupingBy(ContentIdentity::getContentType));

        return contentMap.entrySet()
                .stream()
                .map(entry -> contentProviderFactory
                        .getContentProvider(entry.getKey())
                        .getContentDetails(entry.getValue()))
                .flatMap(List::stream)
                .toList();
    }
}
