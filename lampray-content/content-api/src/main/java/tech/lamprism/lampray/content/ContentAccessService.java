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

package tech.lamprism.lampray.content;

import tech.lamprism.lampray.content.common.ContentException;

/**
 * @author RollW
 */
public interface ContentAccessService {
    /**
     * Get the details of the content with access check.
     *
     * @param contentTrait             the trait of the content
     * @param contentAccessCredentials the credentials to access the content
     * @return the details of the content may need to convert to the specific type.
     * @throws ContentException if the content is not found or the passes are not valid.
     */
    ContentDetails openContent(ContentTrait contentTrait,
                               ContentAccessCredentials contentAccessCredentials)
            throws ContentException;

    /**
     * Get the details of the content with no access check.
     *
     * @throws ContentException if the content is not found.
     * @see #openContent(ContentTrait, ContentAccessCredentials)
     */
    // TODO: merge into #openContent(ContentTrait, ContentAccessCredentials)
    ContentDetails getContentDetails(ContentTrait contentTrait)
            throws ContentException;

    ContentMetadataDetails<?> getContentMetadataDetails(ContentTrait contentTrait)
            throws ContentException;

}
