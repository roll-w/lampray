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

import space.lingu.NonNull;

import java.util.List;

/**
 * Content service internal interface.
 *
 * @author RollW
 */
public interface ContentProvider extends ContentSupportable {
    @Override
    boolean supports(@NonNull ContentType contentType);

    @NonNull
    default ContentDetails getContentDetails(@NonNull ContentTrait contentTrait) {
        return getContentOperator(contentTrait, false);
    }

    @NonNull
    ContentOperator getContentOperator(@NonNull ContentTrait contentTrait,
                                       boolean checkDelete);

    /**
     * Get content details by a list of content traits.
     * <p>
     * This method is used to get content details for multiple content traits at once.
     * It is more efficient than calling {@link #getContentDetails(ContentTrait)} for each trait.
     *
     * @param contentTraits the list of content traits
     * @return the list of content details
     */
    @NonNull
    List<ContentDetails> getContentDetails(@NonNull List<? extends ContentTrait> contentTraits);
}
