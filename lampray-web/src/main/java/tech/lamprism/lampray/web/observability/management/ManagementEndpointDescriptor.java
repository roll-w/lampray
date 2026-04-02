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

package tech.lamprism.lampray.web.observability.management;

import java.util.Objects;

/**
 * @author RollW
 */
public final class ManagementEndpointDescriptor {
    private final String id;
    private final String description;
    private final String produces;
    private final ManagementAccess managementAccess;
    private final String aliasPath;
    private final ManagementAccess aliasAccess;

    public ManagementEndpointDescriptor(String id,
                                       String description,
                                       String produces,
                                       ManagementAccess managementAccess,
                                       String aliasPath,
                                       ManagementAccess aliasAccess) {
        this.id = normalize(id);
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.produces = Objects.requireNonNull(produces, "produces cannot be null");
        this.managementAccess = Objects.requireNonNull(managementAccess, "managementAccess cannot be null");
        this.aliasPath = aliasPath;
        this.aliasAccess = aliasAccess;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getProduces() {
        return produces;
    }

    public ManagementAccess getManagementAccess() {
        return managementAccess;
    }

    public String getAliasPath() {
        return aliasPath;
    }

    public ManagementAccess getAliasAccess() {
        return aliasAccess;
    }

    private String normalize(String value) {
        Objects.requireNonNull(value, "id cannot be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("endpoint id cannot be blank");
        }
        return normalized;
    }
}
