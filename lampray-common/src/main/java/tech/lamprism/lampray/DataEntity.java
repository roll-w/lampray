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

package tech.lamprism.lampray;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.rollw.common.web.system.SystemResource;

import java.io.Serializable;
import java.util.Objects;

/**
 * Indicates a database entity.
 *
 * @author RollW
 */
public interface DataEntity<ID> extends Serializable,
        SystemResource<ID>, TimeAttributed {
    /**
     * Get the id of the entity.
     *
     * @return the id of the entity.
     */
    @Nullable
    ID getEntityId();

    @Override
    @NonNull
    // Resource id must not be null, but use getEntityId() provides a nullable value.
    default ID getResourceId() {
        return Objects.requireNonNull(getEntityId(), "The id of the entity must not be null.");
    }

}