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

package tech.lamprism.lampray.web.observability.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.observability.HealthContributor;
import tech.lamprism.lampray.observability.HealthStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public class DatabaseHealthContributor implements HealthContributor {
    private final ObjectProvider<DataSource> dataSourceProvider;

    public DatabaseHealthContributor(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @Override
    public String getName() {
        return HealthContributorNames.DATABASE;
    }

    @Override
    public Health health() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return Health.down().withDetail("reason", "No DataSource bean available");
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("valid", valid);
            details.put("product", connection.getMetaData().getDatabaseProductName());
            details.put("url", connection.getMetaData().getURL());
            return Health.status(valid ? HealthStatus.UP : HealthStatus.DOWN)
                    .withDetails(details);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
            return Health.down().withDetail("message", message);
        }
    }
}
